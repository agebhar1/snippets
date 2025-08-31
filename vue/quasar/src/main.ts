import '@quasar/extras/material-icons/material-icons.css'
import '@quasar/extras/material-icons-outlined/material-icons-outlined.css'
import 'quasar/src/css/index.sass'

import { Quasar } from 'quasar'
import App from './App.vue'
import { createApp } from 'vue'

createApp(App)
  .use(Quasar)
  .mount('#app')
