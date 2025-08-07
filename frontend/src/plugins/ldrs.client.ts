import { defineNuxtPlugin } from '#app'
import { hatch } from 'ldrs'

export default defineNuxtPlugin(() => {
  hatch.register()
  hatch.register('l-hatch-spinner')
})
