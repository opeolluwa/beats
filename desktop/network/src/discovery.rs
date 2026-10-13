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

pub type PeersMap = Arc<Mutex<HashMap<String, DiscoveredPeer>>>;

pub struct Discovery {
    daemon: ServiceDaemon,
    peers: PeersMap,
    self_fullname: Arc<Mutex<Option<String>>>,
}

impl Discovery {
    pub fn new() -> Result<Self, mdns_sd::Error> {
        let daemon = ServiceDaemon::new()?;
        Ok(Self {
            daemon,
            peers: Arc::new(Mutex::new(HashMap::new())),
            self_fullname: Arc::new(Mutex::new(None)),
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

        let fullname = format!("{}._beats._tcp.local.", instance_name);
        if let Ok(mut name) = self.self_fullname.lock() {
            *name = Some(fullname.clone());
        }

        self.daemon.register(service)?;
        tracing::info!("Registered mDNS service: {} (fullname: {}) at {}:{}", instance_name, fullname, ip, port);
        Ok(())
    }

    pub fn browse(&self) -> Result<(), mdns_sd::Error> {
        let receiver = self.daemon.browse(SERVICE_TYPE)?;
        let peers = Arc::clone(&self.peers);
        let self_fullname = Arc::clone(&self.self_fullname);

        std::thread::spawn(move || {
            while let Ok(event) = receiver.recv() {
                match event {
                    ServiceEvent::ServiceResolved(info) => {
                        let fullname = info.get_fullname().to_string();

                        // Skip our own service
                        if let Ok(self_name) = self_fullname.lock() {
                            if self_name.as_deref() == Some(&fullname) {
                                tracing::debug!("Skipping self: {}", fullname);
                                continue;
                            }
                        }

                        if let Some(addr) = info.get_addresses().iter().next() {
                            let display_name = fullname
                                .strip_suffix("._beats._tcp.local.")
                                .unwrap_or(&fullname)
                                .to_string();
                            let peer = DiscoveredPeer {
                                name: display_name,
                                ip: addr.to_string(),
                                port: info.get_port(),
                            };
                            tracing::info!("Discovered peer: {} at {}:{}", peer.name, peer.ip, peer.port);
                            if let Ok(mut map) = peers.lock() {
                                map.insert(fullname, peer);
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

    pub fn peers_handle(&self) -> PeersMap {
        Arc::clone(&self.peers)
    }

    pub fn shutdown(self) -> Result<(), mdns_sd::Error> {
        self.daemon.shutdown().map(|_| ())
    }
}
