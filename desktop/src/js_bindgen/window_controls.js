export async function minimize_window() {
  await window.__TAURI__.window.getCurrentWindow().minimize();
}

export async function toggle_maximize_window() {
  await window.__TAURI__.window.getCurrentWindow().toggleMaximize();
}

export async function close_window() {
  await window.__TAURI__.window.getCurrentWindow().close();
}
