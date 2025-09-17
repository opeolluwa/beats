import {createApp} from "vue";
import {createPinia} from 'pinia'

import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import App from "./App.vue";
import "./assets/main.css"

import {router} from "./router";
import ui from '@nuxt/ui/vue-plugin'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const app = createApp(App)

app.use(router)
app.use(ui)
app.use(pinia)

app.mount("#app");
