use mdns_sd::{ServiceDaemon, ServiceEvent, ServiceInfo};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::IpAddr;
use std::sync::{Arc, Mutex};

const SERVICE_TYPE: &str = "_beats._tcp.local.";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiscoveredPeer {
    pub name: String,
    pub ip: String,
    pub port: u16,
}

pub struct Discovery {
    daemon: ServiceDaemon,
    peers: Arc<Mutex<HashMap<String, DiscoveredPeer>>>,
}

impl Discovery {
    pub fn new() -> Result<Self, mdns_sd::Error> {
        let daemon = ServiceDaemon::new()?;
        Ok(Self {
            daemon,
            peers: Arc::new(Mutex::new(HashMap::new())),
        })
    }

    pub fn register(&self, device_name: &str, ip: IpAddr, port: u16) -> Result<(), mdns_sd::Error> {
        let host = format!("{}.local.", device_name.to_lowercase().replace(' ', "-"));
        let instance_name = device_name;

        let service = ServiceInfo::new(
            SERVICE_TYPE,
            instance_name,
            &host,
            ip,
            port,
            None,
        )?;

        self.daemon.register(service)?;
        tracing::info!("Registered mDNS service: {} at {}:{}", instance_name, ip, port);
        Ok(())
    }

    pub fn browse(&self) -> Result<(), mdns_sd::Error> {
        let receiver = self.daemon.browse(SERVICE_TYPE)?;
        let peers = Arc::clone(&self.peers);

        tokio::spawn(async move {
            while let Ok(event) = receiver.recv_async().await {
                match event {
                    ServiceEvent::ServiceResolved(info) => {
                        if let Some(addr) = info.get_addresses().iter().next() {
                            let peer = DiscoveredPeer {
                                name: info.get_fullname().to_string(),
                                ip: addr.to_string(),
                                port: info.get_port(),
                            };
                            tracing::info!("Discovered peer: {} at {}:{}", peer.name, peer.ip, peer.port);
                            if let Ok(mut map) = peers.lock() {
                                map.insert(peer.name.clone(), peer);
                            }
                        }
                    }
                    ServiceEvent::ServiceRemoved(_, fullname) => {
                        tracing::info!("Peer removed: {}", fullname);
                        if let Ok(mut map) = peers.lock() {
                            map.remove(&fullname);
                        }
                    }
                    _ => {}
                }
            }
        });

        Ok(())
    }

    pub fn get_peers(&self) -> Vec<DiscoveredPeer> {
        self.peers
            .lock()
            .map(|map| map.values().cloned().collect())
            .unwrap_or_default()
    }

    pub fn shutdown(self) -> Result<(), mdns_sd::Error> {
        self.daemon.shutdown().map(|_| ())
    }
}
