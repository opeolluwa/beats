use crate::config::EmbeddedServerConfig;

#[tauri_interop::command]
pub fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[tauri_interop::command]
pub async fn extract_connection() -> EmbeddedServerConfig {
    EmbeddedServerConfig::default()
}

tauri_interop::collect_commands!();
