<template>
  <CallbackPage />
</template>

<script>
import CallbackPage from '~/components/CallbackPage.vue'
import { githubExchange } from '~/utils/github'

export default {
  name: 'GithubCallbackPageView',
  components: { CallbackPage },
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
  },
}
</script>
