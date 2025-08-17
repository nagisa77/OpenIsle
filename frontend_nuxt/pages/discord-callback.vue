<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { discordExchange } from '~/utils/discord'

onMounted(async () => {
  const url = new URL(window.location.href)
  const code = url.searchParams.get('code')
  const inviteToken = url.searchParams.get('state')
  const result = await discordExchange(code, inviteToken, '')

  if (result.needReason) {
    const q = inviteToken ? `&invite_token=${inviteToken}` : ''
    navigateTo(`/signup-reason?token=${result.token}${q}`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
})
</script>
