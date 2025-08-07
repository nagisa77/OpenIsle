<!-- eslint-disable vue/multi-word-component-names -->
<script setup>
import { navigateTo } from '#app'

if (process.client) {
  navigateTo('/404', { replace: true })
}
</script>
