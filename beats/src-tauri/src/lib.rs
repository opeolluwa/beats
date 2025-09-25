use std::{
    net::{IpAddr, Ipv4Addr},
    sync::Arc,
};

use embedded_server::{local_ip, server::EmbeddedHttpServer, EmbeddedServerConfig};

// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {

    let local_ip = local_ip().unwrap_or(IpAddr::from(Ipv4Addr::UNSPECIFIED));
    let app_state = embedded_server::AppState {
        server_config: EmbeddedServerConfig::default(),
    };

    tauri::async_runtime::spawn(EmbeddedHttpServer::run());

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
