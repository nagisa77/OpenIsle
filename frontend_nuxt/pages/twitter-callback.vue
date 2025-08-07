<template>
  <CallbackPage />
</template>

<script>
import CallbackPage from '../components/CallbackPage.vue'
import { twitterExchange } from '../utils/twitter'

export default {
  name: 'TwitterCallbackPageView',
  components: { CallbackPage },
  async mounted() {
    const url = new URL(window.location.href)
    const code = url.searchParams.get('code')
    const state = url.searchParams.get('state')
    const result = await twitterExchange(code, state, '')

    if (result.needReason) {
      this.$router.push('/signup-reason?token=' + result.token)
    } else {
      this.$router.push('/')
    }
  }
}
</script>

