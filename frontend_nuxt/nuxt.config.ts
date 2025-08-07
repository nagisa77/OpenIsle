import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  ssr: false,
  css: [
    '~/assets/global.css',
    'vue-toastification/dist/index.css',
    '~/assets/toast.css'
  ]
})
