use leptos::prelude::*;
use leptos::view;

use crate::components::pulsing_circle::PulsingCirclesAnimation;

#[leptos::component]
pub fn WelcomeScreen() -> impl leptos::IntoView {
    view! {
        <div class="text-center flex flex-col align-center justify-center items-center h-[90%]">
            <div class="block mx-auto mb-2">
                <PulsingCirclesAnimation />
            </div>

            <h6 class="font-medium leading-2 text-gray-700 dark:text-gray-400 mt-1 mb-5">
                "Waiting for peer connections"
            </h6>
        </div>
    }
}
