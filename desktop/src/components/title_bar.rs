use leptos::prelude::*;
use leptos::view;
use wasm_bindgen_futures::spawn_local;

use crate::js_bindgen::window_controls;
use crate::state::AppState;

#[leptos::component]
pub fn TitleBar() -> impl leptos::IntoView {
    let state = use_context::<AppState>().expect("AppState context");

    let on_close = move |_| {
        spawn_local(async {
            window_controls::close_window().await;
        });
    };
    let on_minimize = move |_| {
        spawn_local(async {
            window_controls::minimize_window().await;
        });
    };
    let on_maximize = move |_| {
        spawn_local(async {
            window_controls::toggle_maximize_window().await;
        });
    };

    let title_text = move || {
        let p = state.peers.get();
        if p.is_empty() {
            "Beats".to_string()
        } else {
            p[0].clone()
        }
    };

    view! {
        <style>
            "
            .titlebar {
                height: 40px;
                display: flex;
                align-items: center;
                padding: 0 12px;
                background: #f8f8f8;
                border-bottom: 1px solid rgba(0, 0, 0, 0.08);
                user-select: none;
                -webkit-user-select: none;
                flex-shrink: 0;
            }

            .titlebar-traffic {
                display: flex;
                align-items: center;
                gap: 8px;
                z-index: 10;
            }

            .traffic-btn {
                width: 12px;
                height: 12px;
                border-radius: 50%;
                border: none;
                cursor: pointer;
                padding: 0;
                transition: filter 0.15s;
            }

            .traffic-btn:hover {
                filter: brightness(0.85);
            }

            .traffic-close { background: #ff5f57; }
            .traffic-minimize { background: #febc2e; }
            .traffic-maximize { background: #28c840; }

            .titlebar-center {
                flex: 1;
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 8px;
                font-size: 13px;
                font-weight: 600;
                color: #333;
            }

            .titlebar-right {
                display: flex;
                align-items: center;
                gap: 6px;
                font-size: 12px;
                font-weight: 500;
                color: #6b7280;
                min-width: 68px;
                justify-content: flex-end;
            }

            .status-dot {
                width: 7px;
                height: 7px;
                border-radius: 50%;
                background: #10b981;
                animation: status-pulse 2s ease-in-out infinite;
            }

            @keyframes status-pulse {
                0%, 100% { opacity: 1; }
                50% { opacity: 0.4; }
            }

            .titlebar-spinner {
                width: 14px;
                height: 14px;
                border: 2px solid rgba(48, 116, 245, 0.2);
                border-top-color: #3074F5;
                border-radius: 50%;
                animation: spin 0.7s linear infinite;
            }

            @media (prefers-color-scheme: dark) {
                .titlebar {
                    background: #1d232a;
                    border-bottom-color: rgba(255, 255, 255, 0.06);
                }
                .titlebar-center { color: #e5e7eb; }
                .titlebar-right { color: #9ca3af; }
            }
            "
        </style>

        <div class="titlebar" data-tauri-drag-region="true">
            <div class="titlebar-traffic">
                <button class="traffic-btn traffic-close" on:click=on_close></button>
                <button class="traffic-btn traffic-minimize" on:click=on_minimize></button>
                <button class="traffic-btn traffic-maximize" on:click=on_maximize></button>
            </div>

            <div class="titlebar-center" data-tauri-drag-region="true">
                {title_text}
            </div>

            <div class="titlebar-right">
                <Show
                    when=move || state.uploading.get()
                    fallback=move || {
                        view! {
                            <Show when=move || !state.peers.get().is_empty()>
                                <span class="status-dot"></span>
                                <span>"Connected"</span>
                            </Show>
                        }
                    }
                >
                    <div class="titlebar-spinner"></div>
                    <span>
                        {move || {
                            let c = state.file_count.get();
                            if c == 1 {
                                "1 file".to_string()
                            } else {
                                format!("{} files", c)
                            }
                        }}
                    </span>
                </Show>
            </div>
        </div>
    }
}
