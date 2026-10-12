use crate::config::EmbeddedServerConfig;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct AppState {
    pub server_config: EmbeddedServerConfig,

}
