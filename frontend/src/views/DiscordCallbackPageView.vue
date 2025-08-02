<template>
  <div class="discord-callback-page">
    <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    <div class="discord-callback-page-text">Magic is happening...</div>
  </div>
</template>

<script>
import { discordExchange } from '../utils/discord'
import { hatch } from 'ldrs'

hatch.register()

export default {
  name: 'DiscordCallbackPageView',
  async mounted() {
    const url = new URL(window.location.href)
    const code = url.searchParams.get('code')
    const state = url.searchParams.get('state')
    const result = await discordExchange(code, state, '')

    if (result.needReason) {
      this.$router.push('/signup-reason?token=' + result.token)
    } else {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.discord-callback-page {
  background-color: var(--background-color);
  height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.discord-callback-page-text {
  margin-top: 25px;
  font-size: 16px;
  color: var(--primary-color);
  font-weight: bold;
}
</style>
