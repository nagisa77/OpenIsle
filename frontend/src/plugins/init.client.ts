import { defineNuxtPlugin } from '#app'
import { checkToken, clearToken, isLogin } from '../utils/auth'
import { loginWithGoogle } from '../utils/google'
import { initTheme } from '../utils/theme'
import { clearVditorStorage } from '../utils/clearVditorStorage'

export default defineNuxtPlugin(async () => {
  initTheme()
  clearVditorStorage()
  const valid = await checkToken()
  if (!valid) {
    clearToken()
  }
  if (!isLogin()) {
    setTimeout(() => {
      loginWithGoogle()
    }, 3000)
  }
})
