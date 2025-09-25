mod config;
mod state;
pub(crate) mod errors;
pub(crate) mod router;
pub(crate) mod routes;

#[cfg(feature = "config")]
pub use config::*;
pub use state::AppState;

pub use local_ip_address::local_ip;
#[cfg(feature = "server")]
pub mod server;
