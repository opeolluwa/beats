use leptos::prelude::*;
use leptos::view;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;
use wasm_bindgen_futures::JsFuture;
use web_sys::{
    console, js_sys, DragEvent, FormData, HtmlInputElement, Request, RequestInit, RequestMode,
    Response,
};

use crate::state::AppState;

async fn fetch_peers() -> Vec<String> {
    let opts = RequestInit::new();
    opts.set_method("GET");
    opts.set_mode(RequestMode::Cors);

    let request = match Request::new_with_str_and_init("http://127.0.0.1:18005/api/peers", &opts) {
        Ok(r) => r,
        Err(_) => return vec![],
    };

    let window = web_sys::window().unwrap();
    let resp = match JsFuture::from(window.fetch_with_request(&request)).await {
        Ok(r) => r,
        Err(e) => {
            console::log_1(&format!("[beats] fetch_peers error: {:?}", e).into());
            return vec![];
        }
    };

    let resp: Response = resp.dyn_into().unwrap();
    let json = match JsFuture::from(resp.json().unwrap()).await {
        Ok(j) => j,
        Err(_) => return vec![],
    };

    let arr = js_sys::Array::from(&json);
    console::log_1(&format!("[beats] peers response: {} items", arr.length()).into());
    let mut names = Vec::new();
    for i in 0..arr.length() {
        let obj = arr.get(i);
        if let Ok(name) = js_sys::Reflect::get(&obj, &JsValue::from_str("name")) {
            if let Some(n) = name.as_string() {
                names.push(n);
            }
        }
    }
    names
}

async fn upload_files(form_data: FormData) -> bool {
    let opts = RequestInit::new();
    opts.set_method("POST");
    opts.set_mode(RequestMode::Cors);
    opts.set_body(&form_data);

    let request =
        match Request::new_with_str_and_init("http://127.0.0.1:18005/api/outbox", &opts) {
            Ok(r) => r,
            Err(_) => return false,
        };

    let window = web_sys::window().unwrap();
    match JsFuture::from(window.fetch_with_request(&request)).await {
        Ok(r) => {
            let resp: Response = r.dyn_into().unwrap();
            resp.ok()
        }
        Err(_) => false,
    }
}

#[leptos::component]
pub fn WelcomeScreen() -> impl leptos::IntoView {
    let state = use_context::<AppState>().expect("AppState context");

    let set_peers = state.set_peers;
    let set_uploading = state.set_uploading;
    let set_upload_msg = state.set_upload_msg;
    let set_upload_ok = state.set_upload_ok;
    let set_file_count = state.set_file_count;

    let peers = state.peers;
    let uploading = state.uploading;
    let upload_msg = state.upload_msg;
    let upload_ok = state.upload_ok;
    let file_count = state.file_count;

    let (drag_over, set_drag_over) = signal(false);

    // Initial fetch
    wasm_bindgen_futures::spawn_local(async move {
        let found = fetch_peers().await;
        set_peers.set(found);
    });

    // Poll every 3s; stop polling when the component unmounts.
    if let Ok(handle) = set_interval_with_handle(
        move || {
            wasm_bindgen_futures::spawn_local(async move {
                let found = fetch_peers().await;
                set_peers.set(found);
            });
        },
        std::time::Duration::from_secs(3),
    ) {
        on_cleanup(move || handle.clear());
    }

    let handle_files = move |file_list: web_sys::FileList| {
        let count = file_list.length();
        if count == 0 {
            return;
        }
        set_file_count.set(count);
        set_uploading.set(true);
        set_upload_msg.set(String::new());
        set_upload_ok.set(false);

        wasm_bindgen_futures::spawn_local(async move {
            let form_data = FormData::new().unwrap();
            for i in 0..file_list.length() {
                if let Some(file) = file_list.get(i) {
                    let _ = form_data.append_with_blob_and_filename("file", &file, &file.name());
                }
            }
            let ok = upload_files(form_data).await;
            set_uploading.set(false);
            set_upload_ok.set(ok);
            if ok {
                set_upload_msg.set("Files staged for transfer".to_string());
            } else {
                set_upload_msg.set("Upload failed — check connection".to_string());
            }
        });
    };

    let handle_files_clone = handle_files.clone();

    let on_drop = move |ev: DragEvent| {
        ev.prevent_default();
        set_drag_over.set(false);
        if let Some(dt) = ev.data_transfer() {
            if let Some(files) = dt.files() {
                handle_files(files);
            }
        }
    };

    let on_drag_over = move |ev: DragEvent| {
        ev.prevent_default();
        set_drag_over.set(true);
    };

    let on_drag_leave = move |ev: DragEvent| {
        ev.prevent_default();
        set_drag_over.set(false);
    };

    let on_input_change = move |ev: leptos::ev::Event| {
        let target = ev.target().unwrap();
        let input: HtmlInputElement = target.dyn_into().unwrap();
        if let Some(files) = input.files() {
            handle_files_clone(files);
        }
        // Allow picking the same file again later.
        input.set_value("");
    };

    let has_peers = move || !peers.get().is_empty();

    view! {
        <style>
            "
            .welcome-root {
                position: relative;
                width: 100%;
                height: 100%;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            /* Drop zone fills the entire area — no border, no background */
            .drop-container {
                position: absolute;
                inset: 0;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
            }

            .drop-container .drop-input {
                position: absolute;
                inset: 0;
                width: 100%;
                height: 100%;
                opacity: 0;
                cursor: pointer;
            }

            .drop-icon {
                width: 48px;
                height: 48px;
                margin-bottom: 16px;
                color: #6b7280;
                opacity: 0.5;
                transition: opacity 0.2s, transform 0.2s;
            }

            .drop-container:hover .drop-icon,
            .drop-container.dragging .drop-icon {
                opacity: 0.55;
                transform: translateY(-2px);
            }

            .drop-label {
                font-size: 15px;
                font-weight: 500;
                color: var(--text-secondary, #6b7280);
                margin-bottom: 4px;
                transition: color 0.2s;
            }

            .drop-hint {
                font-size: 13px;
                color: var(--text-tertiary, #9ca3af);
            }

            .drop-container.dragging .drop-label,
            .drop-container.dragging .drop-icon {
                color: var(--color-app, #3074F5);
            }

            /* Bottom overlay (peers + toast) — lets drops pass through to the zone */
            .bottom-overlay {
                position: absolute;
                left: 0;
                right: 0;
                bottom: 24px;
                display: flex;
                flex-direction: column;
                align-items: center;
                pointer-events: none;
            }

            /* Upload spinner */
            .upload-spinner {
                width: 32px;
                height: 32px;
                border: 2.5px solid rgba(140, 140, 160, 0.15);
                border-top-color: var(--color-app, #3074F5);
                border-radius: 50%;
                animation: spin 0.7s linear infinite;
                margin-bottom: 14px;
            }

            @keyframes spin {
                to { transform: rotate(360deg); }
            }

            .upload-status {
                font-size: 14px;
                font-weight: 500;
                color: var(--text-secondary, #6b7280);
            }

            /* Toast */
            .upload-toast {
                margin-top: 14px;
                padding: 8px 16px;
                border-radius: 8px;
                font-size: 13px;
                font-weight: 500;
                animation: toast-in 0.25s ease-out;
            }

            .upload-toast.success {
                background: rgba(16, 185, 129, 0.08);
                border: 1px solid rgba(16, 185, 129, 0.15);
                color: #10b981;
            }

            .upload-toast.error {
                background: rgba(239, 68, 68, 0.08);
                border: 1px solid rgba(239, 68, 68, 0.15);
                color: #ef4444;
            }

            @keyframes toast-in {
                from { opacity: 0; transform: translateY(4px); }
                to { opacity: 1; transform: translateY(0); }
            }

            /* Waiting state */
            .waiting-container {
                text-align: center;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
            }

            .waiting-label {
                font-size: 14px;
                font-weight: 500;
                color: var(--text-tertiary, #9ca3af);
                margin-top: 20px;
            }

            /* Waiting-for-connection loader */
            .loader {
                width: 140px;
                aspect-ratio: 1;
                padding: 16px;
                box-sizing: border-box;
                background: #fff;
                display: grid;
                filter: blur(5px) contrast(15) hue-rotate(210deg);
                mix-blend-mode: darken;
            }
            .loader:before,
            .loader:after {
                content: '';
                grid-area: 1/1;
                margin: 8px;
                background: #3074F5;
            }
            .loader:after {
                margin: 28px;
                animation: l9 2s infinite;
            }
            @keyframes l9 {
                12.5% { transform: translate(-60%, 60%) }
                25%   { transform: translate( 65%,-65%) }
                37.5% { transform: translate(0, 0) }
                50%   { transform: translate( 60%, 60%) }
                67.5% { transform: translate(-65%,-70%) }
                75%   { transform: translate( 60%,-60%) }
            }

            @media (prefers-color-scheme: dark) {
                .drop-icon { color: #d4d4d8; opacity: 0.6; }
                .drop-label { color: #a1a1aa; }
                .drop-hint { color: #71717a; }
            }
            "
        </style>

        <div class="welcome-root">
            <Show
                when=has_peers
                fallback=move || {
                    view! {
                        <div class="waiting-container">
                            <div class="loader"></div>
                            <p class="waiting-label">"Waiting for connection"</p>
                        </div>
                    }
                }
            >
                <div
                    class=move || {
                        if drag_over.get() {
                            "drop-container dragging"
                        } else {
                            "drop-container"
                        }
                    }
                    on:drop=on_drop
                    on:dragover=on_drag_over
                    on:dragleave=on_drag_leave
                >
                    <div class="flex flex-col items-center justify-center pointer-events-none select-none">
                        <Show
                            when=move || uploading.get()
                            fallback=move || {
                                view! {
                                    <div class="flex flex-col items-center justify-center">
                                        <svg
                                            class="drop-icon"
                                            xmlns="http://www.w3.org/2000/svg"
                                            fill="none"
                                            viewBox="0 0 24 24"
                                            stroke-width="1.5"
                                            stroke="currentColor"
                                        >
                                            <path
                                                stroke-linecap="round"
                                                stroke-linejoin="round"
                                                d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5"
                                            />
                                        </svg>
                                        <p class="drop-label">"Drop files here"</p>
                                        <p class="drop-hint">"or click to browse"</p>
                                    </div>
                                }
                            }
                        >
                            <div class="flex flex-col items-center justify-center">
                                <div class="upload-spinner"></div>
                                <p class="upload-status">
                                    {move || {
                                        let c = file_count.get();
                                        if c == 1 {
                                            "Sending 1 file\u{2026}".to_string()
                                        } else {
                                            format!("Sending {} files\u{2026}", c)
                                        }
                                    }}
                                </p>
                            </div>
                        </Show>
                    </div>

                    <input
                        type="file"
                        multiple=true
                        class="drop-input"
                        on:change=on_input_change
                    />
                </div>

                // Toast floats at the bottom without blocking the drop zone
                <div class="bottom-overlay">
                    <Show when=move || !upload_msg.get().is_empty()>
                        <div class=move || {
                            if upload_ok.get() {
                                "upload-toast success"
                            } else {
                                "upload-toast error"
                            }
                        }>
                            {move || upload_msg.get()}
                        </div>
                    </Show>
                </div>
            </Show>
        </div>
    }
}