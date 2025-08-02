import { ref, computed } from 'vue'

// reactive width value to watch window resize events
const width = ref(window.innerWidth)

// update width on resize
window.addEventListener('resize', () => {
  width.value = window.innerWidth
})

// global computed property
export const isMobile = computed(() => width.value <= 768)
