use wasm_bindgen::prelude::*;

#[wasm_bindgen(module = "/src/js_bindgen/window_controls.js")]
extern "C" {
    pub async fn minimize_window();
    pub async fn toggle_maximize_window();
    pub async fn close_window();
}
