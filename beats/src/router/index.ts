import {createMemoryHistory, createRouter} from 'vue-router'

import DefaultScreen from '../views/defaultScreen.vue'
import UploadScreen from "../views/uploadScreen.vue";
import ProgressScreen from "../views/progressScreen.vue";

const routes = [
    {path: '/', component: DefaultScreen},
    {path: '/upload', component: UploadScreen},
    {path: '/progress', component: ProgressScreen},

]

export const router = createRouter({
    history: createMemoryHistory(),
    routes,
})