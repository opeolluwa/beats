use leptos::prelude::*;
use leptos::view;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;
use wasm_bindgen_futures::JsFuture;
use web_sys::{
    console, js_sys, DragEvent, FormData, HtmlInputElement, Request, RequestInit, RequestMode,
    Response,
};

use crate::components::pulsing_circle::PulsingCirclesAnimation;

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
    let (peers, set_peers) = signal(Vec::<String>::new());
    let (uploading, set_uploading) = signal(false);
    let (upload_msg, set_upload_msg) = signal(String::new());
    let (drag_over, set_drag_over) = signal(false);

    // Initial fetch
    wasm_bindgen_futures::spawn_local(async move {
        let found = fetch_peers().await;
        set_peers.set(found);
    });

    let set_peers_poll = set_peers.clone();
    set_interval_with_handle(
        move || {
            let set_peers = set_peers_poll.clone();
            wasm_bindgen_futures::spawn_local(async move {
                let found = fetch_peers().await;
                set_peers.set(found);
            });
        },
        std::time::Duration::from_secs(3),
    )
    .ok();

    let handle_files = move |file_list: web_sys::FileList| {
        set_uploading.set(true);
        set_upload_msg.set(String::new());

        wasm_bindgen_futures::spawn_local(async move {
            let form_data = FormData::new().unwrap();
            for i in 0..file_list.length() {
                if let Some(file) = file_list.get(i) {
                    let _ = form_data.append_with_blob_and_filename("file", &file, &file.name());
                }
            }
            let ok = upload_files(form_data).await;
            set_uploading.set(false);
            if ok {
                set_upload_msg.set("Files staged for transfer".to_string());
            } else {
                set_upload_msg.set("Upload failed".to_string());
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
    };

    let has_peers = move || !peers.get().is_empty();

    view! {
        <div class="flex flex-col items-center justify-center h-[90%]">
            <Show
                when=has_peers
                fallback=move || {
                    view! {
                        <div class="text-center flex flex-col align-center justify-center items-center">
                            <div class="block mx-auto mb-2">
                                <PulsingCirclesAnimation />
                            </div>
                            <h6 class="font-medium leading-2 text-gray-700 dark:text-gray-400 mt-1 mb-5">
                                "Waiting for peer connections"
                            </h6>
                        </div>
                    }
                }
            >
                <div
                    class=move || {
                        if drag_over.get() {
                            "dropzone dropzone-full dropzone-active"
                        } else {
                            "dropzone dropzone-full"
                        }
                    }
                    on:drop=on_drop
                    on:dragover=on_drag_over
                    on:dragleave=on_drag_leave
                >
                    <div class="flex flex-col items-center justify-center">
                        <Show
                            when=move || uploading.get()
                            fallback=move || {
                                view! {
                                    <div class="flex flex-col items-center justify-center text-gray-400 dark:text-gray-500">
                                        <svg
                                            xmlns="http://www.w3.org/2000/svg"
                                            fill="none"
                                            viewBox="0 0 24 24"
                                            stroke-width="1.5"
                                            stroke="currentColor"
                                            class="size-6 mb-3"
                                        >
                                            <path
                                                stroke-linecap="round"
                                                stroke-linejoin="round"
                                                d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5"
                                            />
                                        </svg>
                                        <p class="text-sm text-gray-500 dark:text-gray-400">"Drop files here or click to browse"</p>
                                        <p class="text-xs text-gray-400 dark:text-gray-500 mt-2">
                                            "Connected to "
                                            {move || {
                                                let p = peers.get();
                                                if p.len() == 1 {
                                                    p[0].clone()
                                                } else {
                                                    format!("{} devices", p.len())
                                                }
                                            }}
                                        </p>
                                    </div>
                                }
                            }
                        >
                            <div class="flex flex-col items-center justify-center text-gray-400">
                                <div class="loader"></div>
                                <p class="mt-4 text-sm">"Uploading..."</p>
                            </div>
                        </Show>
                    </div>

                    <input
                        type="file"
                        multiple=true
                        class="dropzone-input"
                        on:change=on_input_change
                    />

                    <Show when=move || !upload_msg.get().is_empty()>
                        <p class="text-center mt-3 text-sm text-app">{move || upload_msg.get()}</p>
                    </Show>
                </div>
            </Show>
        </div>
    }
}
