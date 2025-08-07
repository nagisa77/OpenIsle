import Toast, { POSITION } from 'vue-toastification'
export default defineNuxtPlugin(nuxtApp => {
  nuxtApp.vueApp.use(Toast, {
    position: POSITION.TOP_RIGHT,
    containerClassName: 'open-isle-toast-style-v1',
    transition: 'Vue-Toastification__fade',
    timeout: 2000
  })
})
