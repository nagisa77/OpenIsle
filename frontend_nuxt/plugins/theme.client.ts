import { defineNuxtPlugin } from 'nuxt/app'
import { initTheme } from '~/utils/theme'

export default defineNuxtPlugin(() => {
  initTheme()
})
