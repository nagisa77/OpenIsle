import { defineNuxtPlugin } from '#app'
import { checkToken, clearToken, isLogin } from '~/utils/auth'
import { loginWithGoogle } from '~/utils/google'
import { initTheme } from '~/utils/theme'
import { clearVditorStorage } from '~/utils/clearVditorStorage'

export default defineNuxtPlugin(() => {
  initTheme()
  clearVditorStorage()
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
})
