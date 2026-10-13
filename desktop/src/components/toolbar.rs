use leptos::prelude::{ClassAttribute, ElementChild};
use leptos::{view, IntoView};

use crate::icons::home_icon::HomeIcon;
use crate::routes::HOME_ROUTE;

#[leptos::component]
pub fn ToolbarItem<F>(
    label: &'static str,
    href: &'static str,
    icon: F,
    #[prop(optional)] class: &'static str,
) -> impl leptos::IntoView
where
    F: IntoView + 'static,
{
    view! {
        <a
            href=href
            class=format!(
                "text-gray-500 flex flex-col justify-center items-center dark:hover:bg-gray-700/40 hover:bg-app-50/50 hover:text-app w-full py-3 ripple-effect ripple-app-500 {}",
                class,
            )
        >
            {icon}
            <span class="sr-only">{label}</span>
        </a>
    }
}

#[leptos::component]
pub fn Toolbar() -> impl leptos::IntoView {
    let home_icon = HomeIcon();

    view! {
        <div class="flex items-center justify-center">
            <ToolbarItem label="Home" href=HOME_ROUTE icon=home_icon class="rounded-full" />
        </div>
    }
}
