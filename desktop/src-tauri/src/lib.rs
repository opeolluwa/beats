use network::discovery::Discovery;
use network::server::EmbeddedHttpServer;
use local_ip_address::local_ip;
use kernel::config::{EmbeddedServerConfig, HTTP_PORT};
use kernel::state::AppState;
use std::{
    net::{IpAddr, Ipv4Addr},
    sync::Arc,
};
use tauri::Manager;
use tauri_plugin_sql::{Migration, MigrationKind};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let migrations = vec![Migration {
        version: 1,
        description: "create_transfer_history_table",
        sql: "CREATE TABLE transfer_history (id TEXT PRIMARY KEY, file_name TEXT, sender TEXT, receiver TEXT,  file_size VARCHAR, date TEXT, status VARCHAR );",
        kind: MigrationKind::Up,
    }];

    let local_ip = local_ip().unwrap_or(IpAddr::from(Ipv4Addr::UNSPECIFIED));
    let app_state = AppState {
        server_config: EmbeddedServerConfig::default(),

    };

    let hostname = hostname::get()
        .map(|h| h.to_string_lossy().to_string())
        .unwrap_or_else(|_| "beats-desktop".to_string());

    let peers = if let Ok(discovery) = Discovery::new() {
        let _ = discovery.register(&hostname, local_ip, HTTP_PORT as u16);
        let _ = discovery.browse();
        let peers = discovery.peers_handle();
        std::mem::forget(discovery);
        peers
    } else {
        Arc::new(std::sync::Mutex::new(std::collections::HashMap::new()))
    };

    tauri::async_runtime::spawn(EmbeddedHttpServer::run(Arc::new(local_ip), peers));

    tauri::Builder::default()
        .setup(move |app| {
            app.manage(app_state);
            Ok(())
        })
        .plugin(
            tauri_plugin_sql::Builder::default()
                .add_migrations("sqlite:beats.db", migrations)
                .build(),
        )
        .plugin(tauri_plugin_os::init())
        .plugin(tauri_plugin_shell::init())
        .invoke_handler(kernel::cmd::get_handlers())
        // .invoke_handler(tauri::generate_handler![])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
