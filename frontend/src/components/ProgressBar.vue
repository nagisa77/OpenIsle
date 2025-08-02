<template>
  <div class="progress-bar">
    <div class="progress-bar-inner" :style="{ width: `${percent}%` }" />
  </div>
</template>

<script>
export default {
  name: 'ProgressBar',
  props: {
    value: { type: Number, default: 0 },
    max: { type: Number, default: 100 }
  },
  computed: {
    percent () {
      if (this.max <= 0) return 0
      const p = (this.value / this.max) * 100
      return Math.max(0, Math.min(100, p))
    }
  }
}
</script>

<style scoped>
.progress-bar {
  width: 200px;
  height: 8px;
  background-color: var(--normal-background-color);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-inner {
  height: 100%;
  background-color: var(--primary-color);
}
</style>
