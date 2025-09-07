<template>
  <div class="new-message-container" :style="{ bottom: bottom + 'px' }" @click="$emit('click')">
    {{ count }} 条新消息，点击查看
  </div>
</template>

<script setup>
const props = defineProps({
  count: {
    type: Number,
    default: 0,
  },
  bottom: {
    type: Number,
    default: 0,
  },
})
</script>

<style scoped>
.new-message-container {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  background-color: var(--primary-color);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 50;
}
</style>
