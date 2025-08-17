<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { googleAuthWithToken } from '~/utils/google'

onMounted(async () => {
  const hash = new URLSearchParams(window.location.hash.substring(1))
  const idToken = hash.get('id_token')
  const inviteToken = hash.get('state')
  if (idToken) {
    await googleAuthWithToken(
      idToken,
      inviteToken,
      () => {
        navigateTo('/', { replace: true })
      },
      (token) => {
        const q = inviteToken ? `&invite_token=${inviteToken}` : ''
        navigateTo(`/signup-reason?token=${token}${q}`, { replace: true })
      },
    )
  } else {
    navigateTo('/login', { replace: true })
  }
})
</script>
