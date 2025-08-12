<template>
  <CallbackPage />
</template>

<script>
import CallbackPage from '~/components/CallbackPage.vue'
import { googleAuthWithToken } from '~/utils/google'

export default {
  name: 'GoogleCallbackPageView',
  components: { CallbackPage },
  async mounted() {
    const hash = new URLSearchParams(window.location.hash.substring(1))
    const idToken = hash.get('id_token')
    if (idToken) {
      await googleAuthWithToken(
        idToken,
        () => {
          this.$router.push('/')
        },
        (token) => {
          this.$router.push('/signup-reason?token=' + token)
        },
      )
    } else {
      this.$router.push('/login')
    }
  },
}
</script>
