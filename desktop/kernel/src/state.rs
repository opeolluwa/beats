use serde::{Deserialize, Serialize};

use crate::config::EmbeddedServerConfig;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppState {
    pub server_config: EmbeddedServerConfig,
}
