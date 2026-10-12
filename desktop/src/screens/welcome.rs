use crate::leptos_dom::logging::console_log;
use leptos::prelude::*;
use leptos::task::spawn_local;
use leptos::view;
use leptos_qr::QrCode;

#[leptos::component]
pub fn WelcomeScreen() -> impl leptos::IntoView {
    let (greet_msg, set_greet_msg) = signal(String::new());
    let (server_config, set_server_config) = signal::<String>(String::new());

    //TODO: add validation to ensure the server is not a loop back address

    spawn_local(async move {
        let result = call_greet("alex").await;
        set_greet_msg.set(result);
        let server_config = cmd_extract_connection().await;
        set_server_config.set(format!("{:#?}", server_config.to_string()));
    });

    view! {
        <div class="text-center flex flex-col align-center justify-center items-center h-[90%]">
             
            <div class="w-[180px] h-[180px] block mx-auto mb-2 rounded-md bg-blend-multiply">
                <QrCode
                    data=server_config
                    ecl=leptos_qr::ECL::Q
                    shape=leptos_qr::Shape::Square
                    bg_color="#f5f7f7"
                    fg_color="#0c0c0c"
                />
            </div>

       <h6 class="font-medium leading-2 text-gray-700 dark:text-gray-400 mt-1 mb-5">
                "Scan QR code to pair mobile device"
            </h6>

            <button class="font-medium leading-2  text-gray-700  mt-3 btn bg-app-500  capitalize shadow-md mt-2 outline-none btn-md text-white block w-[250px] hover:bg-app-400  btn-md">
                refresh
            </button>

        </div>
    }
}

async fn call_greet(name: &str) -> String {
    kernel::cmd::greet(&name).await
}

use kernel::cmd;
use kernel::config::EmbeddedServerConfig;

pub async fn cmd_extract_connection() -> EmbeddedServerConfig {
    cmd::extract_connection().await
}
