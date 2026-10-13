use leptos::prelude::ElementChild;
use leptos::{
    children::Children,
    prelude::{ClassAttribute, StyleAttribute},
    view,
};

use crate::components::title_bar::TitleBar;

#[leptos::component]
pub fn DefaultLayout(children: Children) -> impl leptos::IntoView {
    let children = children();
    view! {
        <div
            style="height:100vh; display:flex; flex-direction:column; overflow:hidden;"
            class="dark:bg-gray-900"
        >
            <TitleBar />
            <main
                class="px-4 py-4 overflow-y-auto"
                style="flex:1; min-height:0;"
            >
                {children}
            </main>
        </div>
    }
}
