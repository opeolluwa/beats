use leptos::*;
use leptos::prelude::*;
use leptos_router::{
    components::{Route, Router, Routes},
    path,
};
use thaw::ConfigProvider;

use crate::screens::welcome::WelcomeScreen;
use crate::{layouts::default_layout::DefaultLayout, screens::home::HomeScreen};
use crate::state::AppState;

#[component]
pub fn App() -> impl leptos::IntoView {
    let app_state = AppState::new();
    provide_context(app_state);

    view! {
        <ConfigProvider class="dark:bg-[#1d232a]">
            <Router>
                <DefaultLayout>
                    <Routes transition=true fallback=|| "Not found.">
                        <Route path=path!("/") view=WelcomeScreen />
                        <Route path=path!("/home") view=HomeScreen />
                    </Routes>

                </DefaultLayout>
            </Router>
        </ConfigProvider>
    }
}
