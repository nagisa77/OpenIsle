import { defineNuxtPlugin } from '#app'
import ClickOutside from '../directives/clickOutside'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('click-outside', ClickOutside)
})
