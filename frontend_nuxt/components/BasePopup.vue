<template>
  <div v-if="visible" class="popup">
    <div class="popup-overlay" @click="onOverlayClick"></div>
    <div class="popup-content">
      <slot />
    </div>
  </div>
</template>

<script>
export default {
  name: 'BasePopup',
  props: {
    visible: { type: Boolean, default: false },
    closeOnOverlay: { type: Boolean, default: true },
  },
  emits: ['close'],
  methods: {
    onOverlayClick() {
      if (this.closeOnOverlay) this.$emit('close')
    },
  },
}
</script>

<style scoped>
.popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}
.popup-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}
.popup-content {
  position: relative;
  z-index: 2;
  background-color: var(--background-color);
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  max-width: 80%;
  max-height: 80%;
  overflow-y: auto;
}
</style>
