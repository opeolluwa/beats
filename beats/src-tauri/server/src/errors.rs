use thiserror::Error;

#[derive(Error, Debug)]
pub enum ServerError {
    #[error("Start up error due to {0}")]
    StartUpError(String),
    #[error("{0}")]
    ConnectionError(String),
}
