use std::net::IpAddr;
use std::net::Ipv4Addr;

use axum::http::Method;

use serde::Deserialize;
use serde::Serialize;
use tower_http::cors::Any;
use tower_http::cors::CorsLayer;
use tower_http::trace::DefaultMakeSpan;
use tower_http::trace::TraceLayer;

use local_ip_address::local_ip;

use axum::extract::DefaultBodyLimit;

use crate::errors::ServerError;
use crate::router;

//TODO:  run the sever can be created with multiple instances or spawn threads

#[derive(Debug, Serialize, Deserialize)]
pub struct EmbeddedHttpServer;

impl EmbeddedHttpServer {
    pub async fn run() -> Result<(), ServerError> {
        tracing_subscriber::fmt()
            .with_max_level(tracing::Level::DEBUG)
            .init();

        let cors_layer = CorsLayer::new()
            .allow_headers(Any) //TODO: add key gen for security
            .allow_methods([Method::GET, Method::POST])
            .allow_origin(Any); //TODO: restrict to IP address

        //TODO: improve data limit it is currently set to define file limit layer as 10GB
        // see information here <https://docs.rs/axum/0.6.2/axum/extract/struct.DefaultBodyLimit.html#%E2%80%A6>
        let file_size_limit = 10 * 1024 * 1024 * 1024;
        let file_limit = DefaultBodyLimit::max(file_size_limit);

        //  run the https server on localhost then feed off the connection using the wifi gateway, the same way Vite/Vue CLI would do the core server
        // this is currently achieved by binding the server to the device default ip address

        let my_local_ip = local_ip().unwrap_or(IpAddr::from(Ipv4Addr::UNSPECIFIED));
        let ip_address = format!("{:?}:{:?}", my_local_ip, 18005);
        let ip_address = ip_address
            .parse::<std::net::SocketAddr>()
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        println!("my local ip is {}", ip_address);
        let app = router::app()
            .layer(file_limit)
            .layer(cors_layer)
            .layer(tower_http::trace::TraceLayer::new_for_http())
            .layer(
                TraceLayer::new_for_http()
                    .make_span_with(DefaultMakeSpan::default().include_headers(true)),
            );

        // run it
        let listener = tokio::net::TcpListener::bind(&ip_address)
            .await
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        tracing::debug!(" the server port is http://{}", ip_address);

        axum::serve(listener, app)
            .await
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        Ok(())
    }
}
