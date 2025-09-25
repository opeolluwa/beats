use axum::{
    routing::{get, post},
    Router,
};

use super::routes::{accept_file_upload, get_file, handle_404, health_check};

/// return all the routes as /api/<route-path>
pub fn app() -> Router {
    Router::new()
        .nest(
            "/api",
            Router::new()
                .route("/upload", post(accept_file_upload))
                .route("/ping", post(accept_file_upload).get(health_check))
                .route("/file", get(get_file)),
        )
        .fallback(handle_404)
}
