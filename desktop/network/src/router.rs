use axum::{
    routing::{get, post},
    Router,
};
use std::sync::{Arc, Mutex};

use super::routes::{
    accept_file_upload, ack_outbox_file, get_file, handle_404, health_check, list_outbox,
    stage_for_outbox, OutboxState,
};

/// return all the routes as /api/<route-path>
pub fn app() -> Router {
    let outbox: OutboxState = Arc::new(Mutex::new(Vec::new()));

    Router::new()
        .nest(
            "/api",
            Router::new()
                .route("/upload", post(accept_file_upload))
                .route("/ping", post(accept_file_upload).get(health_check))
                .route("/file", get(get_file))
                .route("/outbox", get(list_outbox).post(stage_for_outbox))
                .route("/outbox/ack", post(ack_outbox_file)),
        )
        .fallback(handle_404)
        .with_state(outbox)
}
