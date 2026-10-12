use local_ip_address::local_ip;
use serde::{Deserialize, Serialize};
use std::{
    fmt::Display,
    net::{IpAddr, Ipv4Addr},
};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct EmbeddedServerConfig {
    pub ip_address: String,
    pub ports: Ports,
}

impl Display for EmbeddedServerConfig {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "host: {}, ports: {}", self.ip_address, self.ports)
    }
}

impl EmbeddedServerConfig {
    pub fn setup() -> Self {
        Self {
            ip_address: "".to_string(),
            ports: Ports { http: 0, https: 0 },
        }
    }
}

impl Default for EmbeddedServerConfig {
    fn default() -> Self {
        let local_ip = local_ip().unwrap_or(IpAddr::from(Ipv4Addr::UNSPECIFIED));

        Self {
            //TODO: add ports
            ip_address: local_ip.to_string(),
            ports: Ports::default(),
        }
    }
}

pub const HTTP_PORT: u64 = 18005;
pub const HTTPS_PORT: u64 = 18006;
pub const KEY_CHAIN_PATH: &str = "certificates";
#[derive(Clone, Copy, Serialize, Deserialize, Debug)]
pub struct Ports {
    pub http: u16,
    pub https: u16,
}

impl Default for Ports {
    fn default() -> Self {
        Self {
            http: HTTP_PORT as u16,
            https: HTTPS_PORT as u16,
        }
    }
}

impl Display for Ports {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "http:{}, https:{}", self.http, self.https)
    }
}
