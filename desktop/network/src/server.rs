use std::net::IpAddr;
use std::path::PathBuf;
use std::sync::Arc;

use axum::extract::DefaultBodyLimit;
use axum::http::Method;
use axum_server::tls_rustls::RustlsConfig;
use serde::Deserialize;
use serde::Serialize;
use tower_http::cors::Any;
use tower_http::cors::CorsLayer;
use tower_http::trace::DefaultMakeSpan;
use tower_http::trace::TraceLayer;

use kernel::config::KEY_CHAIN_PATH;
use kernel::config::Ports;

use crate::discovery::PeersMap;
use crate::errors::ServerError;
use crate::router;

#[derive(Debug, Serialize, Deserialize)]
pub struct EmbeddedHttpServer;

impl EmbeddedHttpServer {
    pub async fn run(socket_address: Arc<IpAddr>, peers: PeersMap) -> Result<(), ServerError> {
        tracing_subscriber::fmt()
            .with_max_level(tracing::Level::DEBUG)
            .init();

        let cors_layer = CorsLayer::new()
            .allow_headers(Any) //TODO: add key gen for security and middleware
            .allow_methods([Method::GET, Method::POST])
            .allow_origin(Any); //TODO: restrict to IP address

        //TODO: improve data limit it is currently set to define file limit layer as 10GB
        // see information here <https://docs.rs/axum/0.6.2/axum/extract/struct.DefaultBodyLimit.html#%E2%80%A6>
        let file_size_limit = 10 * 1024 * 1024 * 1024;
        let file_limit = DefaultBodyLimit::max(file_size_limit);

        // optional: spawn a second server to redirect http requests to this server
        let ports = Ports::default();
        tokio::spawn(Self::serve_http(
            ports,
            Arc::clone(&socket_address),
            peers.clone(),
        ));

        // configure certificate and private key used by https
        let config = RustlsConfig::from_pem_file(
            PathBuf::from(env!("CARGO_MANIFEST_DIR"))
                .join(KEY_CHAIN_PATH)
                .join("cert.pem"),
            PathBuf::from(env!("CARGO_MANIFEST_DIR"))
                .join(KEY_CHAIN_PATH)
                .join("key.pem"),
        )
        .await
        .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        //  run the https server on localhost then feed off the connection using the wifi gateway, the same way Vite/Vue CLI would do the core server
        // this is currently achieved by binding the server to the device default ip address

        let bind_addr = format!("0.0.0.0:{}", ports.https);
        let bind_addr = bind_addr
            .parse::<std::net::SocketAddr>()
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        println!("HTTPS server on {}", bind_addr);
        let app = router::app(peers)
            .layer(file_limit)
            .layer(cors_layer)
            .layer(tower_http::trace::TraceLayer::new_for_http())
            .layer(
                TraceLayer::new_for_http()
                    .make_span_with(DefaultMakeSpan::default().include_headers(true)),
            );

        tracing::debug!("HTTPS server listening on https://{}", bind_addr);
        axum_server::bind_rustls(bind_addr, config)
            .serve(app.into_make_service())
            .await
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        Ok(())
    }

    async fn serve_http(
        ports: Ports,
        _socket_address: Arc<IpAddr>,
        peers: PeersMap,
    ) -> Result<(), ServerError> {
        let cors_layer = CorsLayer::new()
            .allow_headers(Any)
            .allow_methods([Method::GET, Method::POST])
            .allow_origin(Any);

        let file_size_limit = 10 * 1024 * 1024 * 1024;
        let file_limit = DefaultBodyLimit::max(file_size_limit);

        let app = router::app(peers)
            .layer(file_limit)
            .layer(cors_layer)
            .layer(
                TraceLayer::new_for_http()
                    .make_span_with(DefaultMakeSpan::default().include_headers(true)),
            );

        let bind_addr = format!("0.0.0.0:{}", ports.http);
        let bind_addr = bind_addr
            .parse::<std::net::SocketAddr>()
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        let listener = tokio::net::TcpListener::bind(bind_addr)
            .await
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;
        tracing::debug!("HTTP server listening on http://{}", bind_addr);
        axum::serve(listener, app)
            .await
            .map_err(|err| ServerError::StartUpError(err.to_string()))?;

        Ok(())
    }
}
