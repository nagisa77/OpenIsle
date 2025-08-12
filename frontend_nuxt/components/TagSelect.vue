<template>
  <Dropdown
    v-model="selected"
    :fetch-options="fetchTags"
    multiple
    placeholder="选择标签"
    remote
    :initial-options="mergedOptions"
  >
    <template #option="{ option }">
      <div class="option-container">
        <div class="option-main">
          <template v-if="option.icon">
            <img
              v-if="isImageIcon(option.icon)"
              :src="option.icon"
              class="option-icon"
              :alt="option.name"
            />
            <i v-else :class="['option-icon', option.icon]"></i>
          </template>
          <span>{{ option.name }}</span>
          <span class="option-count" v-if="option.count > 0"> x {{ option.count }}</span>
        </div>
        <div v-if="option.description" class="option-desc">{{ option.description }}</div>
      </div>
    </template>
  </Dropdown>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { API_BASE_URL, toast } from '~/main'
import Dropdown from '~/components/Dropdown.vue'

export default {
  name: 'TagSelect',
  components: { Dropdown },
  props: {
    modelValue: { type: Array, default: () => [] },
    creatable: { type: Boolean, default: false },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const localTags = ref([])
    const providedTags = ref(Array.isArray(props.options) ? [...props.options] : [])

    watch(
      () => props.options,
      (val) => {
        providedTags.value = Array.isArray(val) ? [...val] : []
      },
    )

    const mergedOptions = computed(() => {
      const arr = [...providedTags.value, ...localTags.value]
      return arr.filter((v, i, a) => a.findIndex((t) => t.id === v.id) === i)
    })

    const isImageIcon = (icon) => {
      if (!icon) return false
      return /^https?:\/\//.test(icon) || icon.startsWith('/')
    }

    const buildTagsUrl = (kw = '') => {
      const base = API_BASE_URL || (process.client ? window.location.origin : '')
      const url = new URL('/api/tags', base)

      if (kw) url.searchParams.set('keyword', kw)
      url.searchParams.set('limit', '10')

      return url.toString()
    }

    const fetchTags = async (kw = '') => {
      const defaultOption = { id: 0, name: '无标签' }

      // 1) 先拼 URL（自动兜底到 window.location.origin）
      const url = buildTagsUrl(kw)

      // 2) 拉数据
      let data = []
      try {
        const res = await fetch(url)
        if (res.ok) data = await res.json()
      } catch {
        toast.error('获取标签失败')
      }

      // 3) 合并、去重、可创建
      let options = [...data, ...localTags.value]

      if (
        props.creatable &&
        kw &&
        !options.some((t) => t.name.toLowerCase() === kw.toLowerCase())
      ) {
        options.push({ id: `__create__:${kw}`, name: `创建"${kw}"` })
      }

      options = Array.from(new Map(options.map((t) => [t.id, t])).values())

      // 4) 最终结果
      return [defaultOption, ...options]
    }

    const selected = computed({
      get: () => props.modelValue,
      set: (v) => {
        if (Array.isArray(v)) {
          if (v.includes(0)) {
            emit('update:modelValue', [])
            return
          }
          if (v.length > 2) {
            toast.error('最多选择两个标签')
            return
          }
          v = v.map((id) => {
            if (typeof id === 'string' && id.startsWith('__create__:')) {
              const name = id.slice(11)
              const newId = `__new__:${name}`
              if (!localTags.value.find((t) => t.id === newId)) {
                localTags.value.push({ id: newId, name })
              }
              return newId
            }
            return id
          })
        }
        emit('update:modelValue', v)
      },
    })

    return { fetchTags, selected, isImageIcon, mergedOptions }
  },
}
</script>

<style scoped>
.option-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.option-main {
  display: flex;
  align-items: center;
  gap: 5px;
}

.option-desc {
  font-size: 12px;
  color: #666;
}

.option-count {
  font-weight: bold;
  opacity: 0.4;
}
</style>
