use leptos::prelude::*;
use leptos::view;

#[leptos::component]
pub fn PulsingCirclesAnimation() -> impl leptos::IntoView {
    view! {
        <div class="flex items-center justify-center" style="width: 50px; height: 50px;">
            <div class="loader"></div>
        </div>
    }
}
