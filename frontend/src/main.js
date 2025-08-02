import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/global.css'
import Toast, { POSITION } from 'vue-toastification'
import 'vue-toastification/dist/index.css'
import './assets/toast.css'
// Use Sugar theme from vue-toast-notification for better toast styling.
// If you prefer Bootstrap style, replace with theme-bootstrap.css instead.
import { useToast } from 'vue-toastification'
import { checkToken, clearToken, isLogin } from './utils/auth'
import { initTheme } from './utils/theme'
import { loginWithGoogle } from './utils/google'

// Configurable API domain and port
// export const API_DOMAIN = 'http://127.0.0.1'
// export const API_PORT = 8081

export const API_DOMAIN = 'http://47.82.99.208'
export const API_PORT = 8080

// export const API_BASE_URL = API_PORT ? `${API_DOMAIN}:${API_PORT}` : API_DOMAIN
export const API_BASE_URL = "";
export const GOOGLE_CLIENT_ID = '777830451304-nt8afkkap18gui4f9entcha99unal744.apps.googleusercontent.com'
export const GITHUB_CLIENT_ID = 'Ov23liVkO1NPAX5JyWxJ'
export const DISCORD_CLIENT_ID = '1394985417044000779'
export const TWITTER_CLIENT_ID = 'ZTRTU05KSk9KTTJrTTdrVC1tc1E6MTpjaQ'
export const toast = useToast()

initTheme()

const app = createApp(App)
app.use(router)
app.use(
  Toast, 
  { 
    position: POSITION.TOP_RIGHT,
    containerClassName: "open-isle-toast-style-v1",
    transition: "Vue-Toastification__fade",
    // closeButton: false,
    timeout: 2000,
  }, 
)

app.mount('#app')

checkToken().then(valid => {
  if (!valid) {
    clearToken()
  }

  if (!isLogin()) {
    setTimeout(() => {
      loginWithGoogle()
    }, 3000)
  }
})
