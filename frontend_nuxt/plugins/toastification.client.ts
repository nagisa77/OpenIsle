import { defineNuxtPlugin } from 'nuxt/app'
import 'vue-toastification/dist/index.css'
import '~/assets/toast.css'

export default defineNuxtPlugin(async (nuxtApp) => {
  // 确保只在客户端环境中注册插件
  if (process.client) {
    try {
      // 使用动态导入来避免 CommonJS 模块问题
      const { default: Toast, POSITION } = await import('vue-toastification')

      nuxtApp.vueApp.use(Toast, {
        position: POSITION.TOP_RIGHT,
        containerClassName: 'open-isle-toast-style-v1',
        transition: 'Vue-Toastification__fade',
        timeout: 2000,
      })
    } catch (error) {
      console.warn('Failed to load vue-toastification:', error)
    }
  }
})
