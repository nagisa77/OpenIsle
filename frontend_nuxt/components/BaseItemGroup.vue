<template>
  <div
    class="base-item-group"
    :style="{
      width: `${containerWidth}px`,
      height: `${itemSize}px`,
      '--base-item-group-duration': `${animationDuration}ms`,
    }"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <div
      v-for="(item, index) in items"
      :key="itemKey(item, index)"
      class="base-item-group__item"
      :style="{
        width: `${itemSize}px`,
        height: `${itemSize}px`,
        transform: `translateX(${index * activeGap}px)`,
        zIndex: items.length - index,
      }"
    >
      <slot :item="item" :index="index">
        <BaseImage
          v-if="item && (item.src || typeof item === 'string')"
          class="base-item-group__image"
          :src="typeof item === 'string' ? item : item.src"
          :alt="itemAlt(item, index)"
        />
        <div v-else class="base-item-group__placeholder">{{ placeholderText(item) }}</div>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watchEffect } from 'vue'
import BaseImage from './BaseImage.vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  itemSize: {
    type: Number,
    default: 40,
  },
  collapsedGap: {
    type: Number,
    default: 12,
  },
  expandedGap: {
    type: Number,
    default: null,
  },
  animationDuration: {
    type: Number,
    default: 200,
  },
  itemKeyField: {
    type: String,
    default: 'id',
  },
})

const isHovered = ref(false)

const onMouseEnter = () => {
  isHovered.value = true
}

const onMouseLeave = () => {
  isHovered.value = false
}

const effectiveExpandedGap = computed(() =>
  props.expandedGap == null ? props.itemSize : props.expandedGap,
)

const activeGap = computed(() =>
  isHovered.value ? effectiveExpandedGap.value : props.collapsedGap,
)

const containerWidth = computed(() =>
  props.items.length ? props.itemSize + (props.items.length - 1) * activeGap.value : props.itemSize,
)

watchEffect(() => {
  if (effectiveExpandedGap.value < props.collapsedGap) {
    console.warn('[BaseItemGroup] `expandedGap` should be greater than or equal to `collapsedGap`.')
  }
})

const itemKey = (item, index) => {
  if (item && typeof item === 'object' && props.itemKeyField in item) {
    return item[props.itemKeyField]
  }
  return index
}

const itemAlt = (item, index) => {
  if (item && typeof item === 'object') {
    return item.alt || `item-${index}`
  }
  if (typeof item === 'string') {
    return `item-${index}`
  }
  return 'item'
}

const placeholderText = (item) => {
  if (item == null) return ''
  if (typeof item === 'object' && 'text' in item) return item.text
  return String(item)
}
</script>

<style scoped>
.base-item-group {
  display: flex;
  position: relative;
  transition: width var(--base-item-group-duration) ease;
}

.base-item-group__item {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  overflow: hidden;
  background-color: var(--color-neutral-100, #f0f2f5);
  transition: transform var(--base-item-group-duration) ease;
  box-shadow: 0 0 0 2px var(--color-surface, #fff);
}

.base-item-group__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.base-item-group__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--color-neutral-500, #666);
  background-color: var(--color-neutral-200, #e5e7eb);
}
</style>
