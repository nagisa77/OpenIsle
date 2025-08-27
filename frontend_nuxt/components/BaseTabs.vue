<template>
  <div class="base-tabs-wrapper" @touchstart="onTouchStart" @touchend="onTouchEnd">
    <div class="base-tabs-header">
      <div class="base-tabs-items">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          :class="['base-tab-item', { selected: tab.name === current }]"
          @click="select(tab.name)"
        >
          <i v-if="tab.icon" :class="tab.icon"></i>
          <span>{{ tab.label }}</span>
        </div>
      </div>
      <div class="base-tabs-right">
        <slot name="right"></slot>
      </div>
    </div>
    <div class="base-tabs-content">
      <slot :current="current"></slot>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: undefined },
  tabs: { type: Array, required: true },
  swipe: { type: Boolean, default: true },
})

const emit = defineEmits(['update:modelValue'])

const current = ref(props.modelValue ?? (props.tabs[0] && props.tabs[0].name))

watch(
  () => props.modelValue,
  (val) => {
    if (val !== undefined) current.value = val
  },
)

function select(name) {
  emit('update:modelValue', name)
}

let startX = 0

function onTouchStart(e) {
  if (!props.swipe) return
  startX = e.changedTouches[0].clientX
}

function onTouchEnd(e) {
  if (!props.swipe) return
  const endX = e.changedTouches[0].clientX
  const diff = endX - startX
  if (Math.abs(diff) > 50) {
    const index = props.tabs.findIndex((t) => t.name === current.value)
    if (diff < 0 && index < props.tabs.length - 1) {
      emit('update:modelValue', props.tabs[index + 1].name)
    } else if (diff > 0 && index > 0) {
      emit('update:modelValue', props.tabs[index - 1].name)
    }
  }
}
</script>

<style scoped>
.base-tabs-wrapper {
  display: flex;
  flex-direction: column;
}

.base-tabs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--normal-border-color);
}

.base-tabs-items {
  display: flex;
  overflow-x: auto;
  scrollbar-width: none;
}

.base-tab-item {
  padding: 10px 20px;
  cursor: pointer;
  white-space: nowrap;
}

.base-tab-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.base-tab-item i {
  margin-right: 6px;
}

.base-tabs-right {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}
</style>
