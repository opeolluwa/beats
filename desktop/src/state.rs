use leptos::prelude::*;

#[derive(Clone, Copy)]
pub struct AppState {
    pub peers: ReadSignal<Vec<String>>,
    pub set_peers: WriteSignal<Vec<String>>,
    pub uploading: ReadSignal<bool>,
    pub set_uploading: WriteSignal<bool>,
    pub file_count: ReadSignal<u32>,
    pub set_file_count: WriteSignal<u32>,
    pub upload_ok: ReadSignal<bool>,
    pub set_upload_ok: WriteSignal<bool>,
    pub upload_msg: ReadSignal<String>,
    pub set_upload_msg: WriteSignal<String>,
}

impl AppState {
    pub fn new() -> Self {
        let (peers, set_peers) = signal(Vec::<String>::new());
        let (uploading, set_uploading) = signal(false);
        let (file_count, set_file_count) = signal(0u32);
        let (upload_ok, set_upload_ok) = signal(false);
        let (upload_msg, set_upload_msg) = signal(String::new());

        Self {
            peers,
            set_peers,
            uploading,
            set_uploading,
            file_count,
            set_file_count,
            upload_ok,
            set_upload_ok,
            upload_msg,
            set_upload_msg,
        }
    }
}
