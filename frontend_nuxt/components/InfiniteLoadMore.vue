<template>
  <!-- 当 done 后整个容器自动隐藏，不再占位 -->
  <div v-show="!done" class="infinite-loadmore">
    <div v-show="isLoading" class="loading-container bottom-loading" aria-live="polite">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <!-- 永久存在的底部触发器（由组件内部持有与观察） -->
    <div ref="sentinel" class="load-more-trigger" aria-hidden="true"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'

const props = defineProps({
  /** 父组件提供：执行“加载下一页”的函数
   * 返回：
   *  - boolean：true 表示“已经没有更多数据（done）”
   *  - { done: boolean }：同上
   */
  onLoad: { type: Function, required: true },
  /** pause=true 时暂停观察（例如首屏/筛选加载过程） */
  pause: { type: Boolean, default: false },
  /** 预取范围，默认 200px */
  rootMargin: { type: String, default: '200px 0px' },
  /** 触发阈值 */
  threshold: { type: Number, default: 0 },
  /** 可选：指定滚动容器 */
  root: { type: [Object, Function], default: null },
})

const isLoading = ref(false)
const done = ref(false)
const sentinel = ref(null)
let io = null

const stopObserver = () => {
  if (io) {
    io.disconnect()
    io = null
  }
}

const startObserver = () => {
  if (!import.meta.client || props.pause || done.value) return
  stopObserver()
  const getRoot = () => {
    if (!props.root) return null
    if (typeof props.root === 'function') return props.root()
    if (props.root && 'value' in props.root) return props.root.value
    return props.root
  }

  io = new IntersectionObserver(
    async (entries) => {
      const e = entries[0]
      if (!e?.isIntersecting || isLoading.value || done.value) return
      isLoading.value = true
      try {
        const res = await props.onLoad()
        const finished = typeof res === 'boolean' ? res : !!(res && res.done)
        if (finished) {
          done.value = true
          stopObserver()
        }
      } finally {
        isLoading.value = false
      }
    },
    { root: getRoot(), rootMargin: props.rootMargin, threshold: props.threshold },
  )
  if (sentinel.value) io.observe(sentinel.value)
}

onMounted(() => {
  nextTick(startObserver)
})
onBeforeUnmount(stopObserver)

watch(
  () => props.pause,
  (p) => {
    if (p) stopObserver()
    else nextTick(startObserver)
  },
)

/** 父组件可选择性调用，用于外部强制重置（一般直接用 :key 重建更简单） */
const reset = () => {
  done.value = false
  nextTick(startObserver)
}
defineExpose({ reset })
</script>

<style scoped>
.infinite-loadmore {
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100px; /* 与原样式匹配 */
}

.load-more-trigger {
  width: 100%;
  height: 1px;
}
</style>
