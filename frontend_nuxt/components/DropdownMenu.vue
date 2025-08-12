<template>
  <div class="dropdown-wrapper" ref="wrapper">
    <div class="dropdown-trigger" @click="toggle">
      <slot name="trigger"></slot>
    </div>
    <div v-if="visible" class="dropdown-menu-container">
      <div
        v-for="(item, idx) in items"
        :key="idx"
        class="dropdown-item"
        :style="{ color: item.color || 'inherit' }"
        @click="handle(item)"
      >
        {{ item.text }}
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
export default {
  name: 'DropdownMenu',
  props: {
    items: { type: Array, default: () => [] },
  },
  setup(props, { expose }) {
    const visible = ref(false)
    const wrapper = ref(null)
    const toggle = () => {
      visible.value = !visible.value
    }
    const close = () => {
      visible.value = false
    }
    const handle = (item) => {
      close()
      if (item && typeof item.onClick === 'function') {
        item.onClick()
      }
    }
    const clickOutside = (e) => {
      if (wrapper.value && !wrapper.value.contains(e.target)) {
        close()
      }
    }
    onMounted(() => {
      document.addEventListener('click', clickOutside)
    })
    onBeforeUnmount(() => {
      document.removeEventListener('click', clickOutside)
    })
    expose({ close })
    return { visible, toggle, wrapper, handle }
  },
}
</script>

<style scoped>
.dropdown-wrapper {
  position: relative;
  display: inline-block;
}
.dropdown-trigger {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.dropdown-menu-container {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: var(--menu-background-color);
  border: 1px solid var(--normal-border-color);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  min-width: 100px;
  z-index: 2000;
}

.dropdown-item {
  padding: 8px 16px;
  white-space: nowrap;
}
.dropdown-item:hover {
  background-color: var(--menu-selected-background-color);
}
</style>
