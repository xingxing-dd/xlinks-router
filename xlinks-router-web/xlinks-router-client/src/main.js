import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import i18n, { getLocale, setLocale } from './locales'
import App from './App.vue'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

setLocale(getLocale())

app.mount('#app')
