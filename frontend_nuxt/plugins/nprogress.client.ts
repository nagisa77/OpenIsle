import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { defineNuxtPlugin } from 'nuxt/app'

export default defineNuxtPlugin((nuxtApp) => {
  NProgress.configure({ showSpinner: false })

  nuxtApp.hook('page:start', () => {
    NProgress.start()
  })

  nuxtApp.hook('page:finish', () => {
    NProgress.done()
  })

  nuxtApp.hook('app:error', () => {
    NProgress.done()
  })
})
