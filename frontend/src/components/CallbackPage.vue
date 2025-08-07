<template>
  <div class="callback-page">
    <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    <div class="callback-page-text">Magic is happening...</div>
  </div>
</template>

<script>
export default {
  name: 'CallbackPage'
}
</script>

<style scoped>
.callback-page {
  background-color: var(--background-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.callback-page-text {
  margin-top: 25px;
  font-size: 16px;
  color: var(--primary-color);
  font-weight: bold;
}
</style>
