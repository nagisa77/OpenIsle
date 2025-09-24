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
          <template v-if="option.smallIcon || option.icon">
            <BaseImage
              v-if="isImageIcon(option.smallIcon || option.icon)"
              :src="option.smallIcon || option.icon"
              class="option-icon"
              :alt="option.name"
            />
            <component v-else :is="option.smallIcon || option.icon" class="option-icon" />
          </template>
          <span>{{ option.name }}</span>
          <span class="option-count" v-if="option.count > 0"> x {{ option.count }}</span>
        </div>
        <div v-if="option.description" class="option-desc">{{ option.description }}</div>
      </div>
    </template>
    <template #footer>
      <div v-if="tagPagination.hasNext" class="dropdown-footer">
        <button
          type="button"
          class="dropdown-more"
          :disabled="isLoadingMore"
          @click.stop.prevent="loadMoreTags"
        >
          <span v-if="!isLoadingMore">查看更多</span>
          <span v-else>加载中...</span>
        </button>
      </div>
    </template>
  </Dropdown>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { toast } from '~/main'
import Dropdown from '~/components/Dropdown.vue'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const TAG_PAGE_SIZE = 10
const defaultOption = { id: 0, name: '无标签' }

const emit = defineEmits(['update:modelValue'])
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  creatable: { type: Boolean, default: false },
  options: { type: Array, default: () => [] },
})

const localTags = ref([])
const providedTags = ref(Array.isArray(props.options) ? [...props.options] : [])
const remoteTags = ref([])
const isLoadingMore = ref(false)
const tagPagination = reactive({
  keyword: '',
  page: 0,
  pageSize: TAG_PAGE_SIZE,
  hasNext: false,
  total: 0,
})

watch(
  () => props.options,
  (val) => {
    providedTags.value = Array.isArray(val) ? [...val] : []
  },
)

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}

const buildTagsUrl = (kw = '', page = 0) => {
  const base = API_BASE_URL || (import.meta.client ? window.location.origin : '')
  const url = new URL('/api/tags', base)

  if (kw) url.searchParams.set('keyword', kw)
  url.searchParams.set('page', String(page))
  url.searchParams.set('pageSize', String(tagPagination.pageSize))

  return url.toString()
}

const fetchTags = async (kw = '') => {
  if (tagPagination.keyword !== kw) {
    tagPagination.keyword = kw
    tagPagination.page = 0
  }

  const url = buildTagsUrl(kw, 0)

  let pageData
  try {
    const res = await fetch(url)
    if (!res.ok) throw new Error('failed to fetch tags')
    pageData = await res.json()
  } catch {
    toast.error('获取标签失败')
    remoteTags.value = []
    tagPagination.hasNext = false
    tagPagination.total = 0
    return buildOptions([])
  }

  const items = Array.isArray(pageData?.items) ? pageData.items : []
  remoteTags.value = items
  tagPagination.page = pageData?.page ?? 0
  tagPagination.pageSize = pageData?.pageSize ?? TAG_PAGE_SIZE
  tagPagination.hasNext = Boolean(pageData?.hasNext)
  tagPagination.total = pageData?.total ?? items.length

  return buildOptions(items)
}

const buildOptions = (remote = remoteTags.value) => {
  let options = [...remote, ...localTags.value]
  if (props.creatable && tagPagination.keyword) {
    const lowerKw = tagPagination.keyword.toLowerCase()
    if (!options.some((t) => typeof t.name === 'string' && t.name.toLowerCase() === lowerKw)) {
      options.push({
        id: `__create__:${tagPagination.keyword}`,
        name: `创建"${tagPagination.keyword}"`,
      })
    }
  }
  options = [...providedTags.value, ...options]
  options = Array.from(new Map(options.map((t) => [t.id, t])).values())
  return [defaultOption, ...options]
}

const mergedOptions = computed(() => buildOptions(remoteTags.value))

const loadMoreTags = async () => {
  if (!tagPagination.hasNext || isLoadingMore.value) return
  isLoadingMore.value = true
  try {
    const nextPage = tagPagination.page + 1
    const url = buildTagsUrl(tagPagination.keyword, nextPage)
    const res = await fetch(url)
    if (!res.ok) throw new Error('failed to fetch tags')
    const pageData = await res.json()
    const items = Array.isArray(pageData?.items) ? pageData.items : []
    remoteTags.value = [...remoteTags.value, ...items]
    tagPagination.page = pageData?.page ?? nextPage
    tagPagination.pageSize = pageData?.pageSize ?? tagPagination.pageSize
    tagPagination.hasNext = Boolean(pageData?.hasNext)
    tagPagination.total = pageData?.total ?? tagPagination.total
  } catch {
    toast.error('获取标签失败')
  } finally {
    isLoadingMore.value = false
  }
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

.dropdown-footer {
  display: flex;
  justify-content: center;
  padding: 8px 0 12px;
}

.dropdown-more {
  border: none;
  background: transparent;
  color: var(--primary-color);
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
}

.dropdown-more[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
