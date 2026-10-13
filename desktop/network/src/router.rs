use std::sync::{Arc, Mutex};

use axum::{
    routing::{get, post},
    Router,
};

use super::routes::{
    accept_file_upload, ack_outbox_file, get_file, handle_404, health_check, list_outbox,
    list_peers, stage_for_outbox, OutboxState,
};
use crate::discovery::PeersMap;

#[derive(Clone)]
pub struct AppState {
    pub outbox: OutboxState,
    pub peers: PeersMap,
}

/// return all the routes as /api/<route-path>
pub fn app(peers: PeersMap) -> Router {
    let outbox: OutboxState = Arc::new(Mutex::new(Vec::new()));
    let state = AppState { outbox, peers };

    Router::new()
        .nest(
            "/api",
            Router::new()
                .route("/upload", post(accept_file_upload))
                .route("/ping", post(accept_file_upload).get(health_check))
                .route("/file", get(get_file))
                .route("/peers", get(list_peers))
                .route("/outbox", get(list_outbox).post(stage_for_outbox))
                .route("/outbox/ack", post(ack_outbox_file)),
        )
        .fallback(handle_404)
        .with_state(state)
}
