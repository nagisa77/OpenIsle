// plugins/ldrs.client.ts
import { defineNuxtPlugin } from 'nuxt/app'

export default defineNuxtPlugin(async () => {
  // 动态引入，防止打包时把 ldrs 拉进 SSR bundle
  const { hatch, helix, spiral } = await import('ldrs')

  // 想用几个就注册几个
  hatch.register()
  helix.register()
  spiral.register()
})
