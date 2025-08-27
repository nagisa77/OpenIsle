<template>
  <NuxtImg
    :src="src"
    :alt="alt"
    :placeholder="placeholder"
    placeholder-class="ph"
    :loading="loading"
    @load="loaded = true"
    :class="['base-img', { 'is-loaded': loaded }]"
    v-bind="$attrs"
  />
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  loading: { type: String, default: 'lazy' },
  placeholderOptions: {
    type: Object,
    default: () => ({ w: 16, h: 16, f: 'webp', q: 40, blur: 2 }),
  },
})

const img = useImage()
const loaded = ref(false)
const placeholder = computed(() => img(props.src, props.placeholderOptions))
</script>

<style scoped>
.base-img {
  opacity: 0;
  transition: opacity 0.25s;
}
.base-img.is-loaded {
  opacity: 1;
}
:deep(img.ph) {
  filter: blur(10px);
  transform: scale(1.03);
}
</style>
