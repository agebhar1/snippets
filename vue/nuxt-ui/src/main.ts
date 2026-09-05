import { createRouter, createWebHistory } from 'vue-router'
import { createApp } from 'vue'
import ui from '@nuxt/ui/vue-plugin'

import App from './App.vue'

import './assets/css/main.css'

const app = createApp(App)

const router = createRouter({
  routes: [],
  history: createWebHistory(),
})

app.use(router)
app.use(ui)

app.mount('#app')
