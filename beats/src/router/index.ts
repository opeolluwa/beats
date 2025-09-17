import {createMemoryHistory, createRouter} from 'vue-router'
import UploadScreen from "../views/uploadScreen.vue";
import ProgressScreen from "../views/progressScreen.vue";
// import WaitingForConnection from "../views/waitingForConnection.vue";

const routes = [
    // {path: '/', component: WaitingForConnection},
    {path: '/', component: UploadScreen},
    {path: '/progress', component: ProgressScreen},
    {path: '/progress', component: ProgressScreen},

]

export const router = createRouter({
    history: createMemoryHistory(),
    routes,
})