<template>
  <div class="github-callback-page">
    <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    <div class="github-callback-page-text">Magic is happening...</div>
  </div>
</template>

<script>
import { githubExchange } from '../utils/github'
import { hatch } from 'ldrs'
hatch.register()


export default {
  name: 'GithubCallbackPageView',
  async mounted() {
    const url = new URL(window.location.href)
    const code = url.searchParams.get('code')
    const state = url.searchParams.get('state')
    const result = await githubExchange(code, state, '')

    if (result.needReason) {
      this.$router.push('/signup-reason?token=' + result.token)
    } else {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.github-callback-page {
  background-color: var(--background-color);
  height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.github-callback-page-text {
  margin-top: 25px;
  font-size: 16px;
  color: var(--primary-color);
  font-weight: bold;
}
</style>
