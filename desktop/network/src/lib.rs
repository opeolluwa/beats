pub(crate) mod errors;
pub(crate) mod router;
pub(crate) mod routes;
pub mod discovery;

#[cfg(feature = "config")]
pub use kernel::config;
#[cfg(feature = "server")]
pub mod server;
