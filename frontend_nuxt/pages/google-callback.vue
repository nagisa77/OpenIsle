<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { googleAuthWithToken } from '~/utils/google'

onMounted(async () => {
  const hash = new URLSearchParams(window.location.hash.substring(1))
  const idToken = hash.get('id_token')
  const state = hash.get('state') || ''
  if (idToken) {
    await googleAuthWithToken(
      idToken,
      () => {
        navigateTo('/', { replace: true })
      },
      (token) => {
        navigateTo(`/signup-reason?token=${token}`, { replace: true })
      },
      state,
    )
  } else {
    navigateTo('/login', { replace: true })
  }
})
</script>
