use axum::body::{Body, Bytes};
use axum::extract::{Multipart, Query, State};
use axum::http::StatusCode;
use axum::response::IntoResponse;
use axum::{BoxError, Json};
use futures::stream::{Stream, TryStreamExt};

use http::header;
use serde::{Deserialize, Serialize};
use serde_json::json;
use serde_json::Value;
use std::fs;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use tokio_util::io::{ReaderStream, StreamReader};

use tokio::fs::File;
use tokio::io::{self, BufWriter};

#[derive(Debug, Serialize, Deserialize)]
pub struct QueryParams {
    pub file_path: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OutboxEntry {
    pub id: String,
    pub name: String,
    pub size: u64,
    pub path: String,
}

#[derive(Debug, Deserialize)]
pub struct AckRequest {
    pub id: String,
}

pub type OutboxState = Arc<Mutex<Vec<OutboxEntry>>>;

pub async fn accept_file_upload(
    mut multipart: Multipart,
) -> Result<(StatusCode, Json<Value>), (StatusCode, String)> {
    while let Some(field) = multipart.next_field().await.unwrap() {
        let file_name = if let Some(file_name) = field.file_name() {
            file_name.to_owned()
        } else {
            continue;
        };

        stream_to_file(&file_name, field).await?;
    }

    Ok((
        StatusCode::OK,
        Json(json!({
            "Success":true,
            "message":"file saved"
        })),
    ))
}

// Save a `Stream` to a file
async fn stream_to_file<S, E>(path: &str, stream: S) -> Result<(), (StatusCode, String)>
where
    S: Stream<Item = Result<Bytes, E>>,
    E: Into<BoxError>,
{
    if !path_is_valid(path) {
        return Err((StatusCode::BAD_REQUEST, "Invalid path".to_owned()));
    }

    async {
        // Convert the stream into an `AsyncRead`.
        let body_with_io_error = stream.map_err(|err| io::Error::new(io::ErrorKind::Other, err));
        let body_reader = StreamReader::new(body_with_io_error);
        futures::pin_mut!(body_reader);

        //create wi-share directory in the downloads path dir and / save files to $DOWNLOADS/wi-share
        let os_default_downloads_dir = dirs::download_dir().unwrap();
        let upload_path = format!(
            "{downloads_dir}/{upload_dir}",
            downloads_dir = os_default_downloads_dir.display(),
            upload_dir = "beats"
        );
        // create the uploads path if not exist
        let _ = fs::create_dir_all(&upload_path);

        // Create the file. `File` implements `AsyncWrite`.
        let path = std::path::Path::new(&upload_path).join(path);
        let mut file = BufWriter::new(File::create(path).await?);

        // Copy the body into the file.
        tokio::io::copy(&mut body_reader, &mut file).await?;

        Ok::<_, io::Error>(())
    }
    .await
    .map_err(|err| (StatusCode::INTERNAL_SERVER_ERROR, err.to_string()))
}

// to prevent directory traversal attacks we ensure the path consists of exactly one normal
// component
fn path_is_valid(path: &str) -> bool {
    let path = std::path::Path::new(path);
    let mut components = path.components().peekable();

    if let Some(first) = components.peek() {
        if !matches!(first, std::path::Component::Normal(_)) {
            return false;
        }
    }

    components.count() == 1
}

// 404 handler
pub async fn handle_404() -> impl IntoResponse {
    (
        StatusCode::NOT_FOUND,
        axum::response::Json(serde_json::json!({
        "success":false,
        "message":String::from("The requested resource does not exist on this server!"),
        })),
    )
}

/// health check handler
pub async fn health_check() -> impl IntoResponse {
    (
        StatusCode::OK,
        axum::response::Json(serde_json::json!({
        "success":true,
        "message":String::from("Server is ready to accept connection"),
        })),
    )
}

/// for a given file path, return the file the the used as a downloadable one
pub async fn get_file(Query(QueryParams { file_path }): Query<QueryParams>) -> impl IntoResponse {
    // `File` implements `AsyncRead`
    let file = match tokio::fs::File::open(file_path.clone()).await {
        Ok(file) => file,
        Err(_err) => return Err((StatusCode::NOT_FOUND, "File not found:".to_string())),
    };

    // convert the `AsyncRead` into a `Stream`
    let stream = ReaderStream::new(file);
    // convert the `Stream` into an `axum::body::HttpBody`
    let body = Body::from_stream(stream);

    let content_type = match mime_guess::from_path(file_path.clone()).first_raw() {
        Some(mime) => mime,
        None => {
            return Err((
                StatusCode::BAD_REQUEST,
                "MIME Type couldn't be determined".to_string(),
            ))
        }
    };

    let file_buf_path = PathBuf::from(file_path.clone());
    let file_metadata = file_buf_path.file_name();

    let filename = match file_metadata {
        Some(name) => name.to_str(),
        None => {
            return Err((
                StatusCode::BAD_REQUEST,
                "File name couldn't be determined".to_string(),
            ))
        }
    };

    let content_disposition = format!("attachment; filename=\"{:?}\"", filename.unwrap());

    let headers = [
        (header::CONTENT_TYPE, content_type),
        (
            header::CONTENT_DISPOSITION,
            content_disposition.as_str(), //    content_disposition
        ),
    ];

    Ok((headers, body).into_response())
}

fn outbox_dir() -> PathBuf {
    let dir = dirs::download_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("beats")
        .join("outbox");
    let _ = fs::create_dir_all(&dir);
    dir
}

pub async fn list_outbox(
    State(outbox): State<OutboxState>,
) -> impl IntoResponse {
    let entries = outbox.lock().unwrap().clone();
    (StatusCode::OK, Json(serde_json::to_value(entries).unwrap()))
}

pub async fn stage_for_outbox(
    State(outbox): State<OutboxState>,
    mut multipart: Multipart,
) -> Result<(StatusCode, Json<Value>), (StatusCode, String)> {
    let dir = outbox_dir();

    while let Some(field) = multipart.next_field().await.unwrap() {
        let file_name = if let Some(name) = field.file_name() {
            name.to_owned()
        } else {
            continue;
        };

        if !path_is_valid(&file_name) {
            return Err((StatusCode::BAD_REQUEST, "Invalid filename".to_owned()));
        }

        let dest = dir.join(&file_name);
        let body_with_io_error =
            field.map_err(|err| io::Error::new(io::ErrorKind::Other, err));
        let body_reader = StreamReader::new(body_with_io_error);
        futures::pin_mut!(body_reader);

        let file = File::create(&dest)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
        let mut writer = BufWriter::new(file);
        tokio::io::copy(&mut body_reader, &mut writer)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

        let size = fs::metadata(&dest).map(|m| m.len()).unwrap_or(0);
        let id = uuid::Uuid::new_v4().to_string();

        outbox.lock().unwrap().push(OutboxEntry {
            id,
            name: file_name,
            size,
            path: dest.to_string_lossy().to_string(),
        });
    }

    Ok((
        StatusCode::OK,
        Json(json!({"success": true, "message": "File staged for transfer"})),
    ))
}

pub async fn ack_outbox_file(
    State(outbox): State<OutboxState>,
    Json(ack): Json<AckRequest>,
) -> impl IntoResponse {
    let mut entries = outbox.lock().unwrap();
    if let Some(pos) = entries.iter().position(|e| e.id == ack.id) {
        let entry = entries.remove(pos);
        let _ = fs::remove_file(&entry.path);
        (
            StatusCode::OK,
            Json(json!({"success": true, "message": "Acknowledged"})),
        )
    } else {
        (
            StatusCode::NOT_FOUND,
            Json(json!({"success": false, "message": "Entry not found"})),
        )
    }
}

#[cfg(test)]
mod basic_endpoints {
    use crate::router;

    use axum::{
        body::Body,
        http::{Request, StatusCode},
    };
    use tower::ServiceExt;
    #[tokio::test]
    async fn ping_endpoint() {
        let app = router::app();

        let response = app
            .oneshot(Request::builder().uri("/api/ping").body(Body::empty()).unwrap())
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::OK);
    }

    // 404 path--
    #[tokio::test]
    async fn not_found_handler() {
        let app = router::app();

        // the 404 handle should return this json
        // it will return a NOT_FOUND  status code
        // the test will test for the validity of  this.
        let response = app
            .oneshot(
                Request::builder()
                    .uri("/not-found-error")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();

        // assert  the the status code is 404
        assert_eq!(response.status(), StatusCode::NOT_FOUND);
    }
}
