import { defineNuxtPlugin } from '#app'
import { initTheme } from '~/utils/theme'

export default defineNuxtPlugin(() => {
  initTheme()
})

