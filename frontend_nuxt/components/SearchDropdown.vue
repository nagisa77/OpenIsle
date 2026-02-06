<template>
  <div class="search-dropdown">
    <Dropdown
      ref="dropdown"
      v-model="selected"
      :fetch-options="fetchResults"
      remote
      menu-class="search-menu"
      option-class="search-option"
      :show-search="isMobile"
      @update:search="keyword = $event"
      @close="onClose"
    >
      <template #display="{ setSearch }">
        <div class="search-input">
          <search-icon class="search-input-icon" />
          <input
            class="text-input"
            v-model="keyword"
            placeholder="键盘点击「/」以触发搜索"
            ref="searchInput"
            @input="setSearch(keyword)"
          />
        </div>
      </template>
      <template #option="{ option }">
        <div class="search-option-item">
          <component :is="iconMap[option.type]" class="result-icon" />
          <div class="result-body">
            <div
              class="result-main"
              v-html="renderHighlight(option.highlightedText, option.text)"
            ></div>
            <div
              v-if="option.subText"
              class="result-sub"
              v-html="renderHighlight(option.highlightedSubText, option.subText)"
            ></div>
            <div
              v-if="option.extra"
              class="result-extra"
              v-html="renderHighlight(option.highlightedExtra, option.extra)"
            ></div>
          </div>
        </div>
      </template>
    </Dropdown>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Dropdown from '~/components/Dropdown.vue'
import { stripMarkdown } from '~/utils/markdown'
import { useIsMobile } from '~/utils/screen'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const emit = defineEmits(['close'])

const keyword = ref('')
const selected = ref(null)
const results = ref([])
const dropdown = ref(null)
const searchInput = ref(null)
const isMobile = useIsMobile()

const isEditableElement = (el) => {
  if (!el) return false
  if (el.isContentEditable) return true
  const tagName = el.tagName ? el.tagName.toLowerCase() : ''
  if (tagName === 'input' || tagName === 'textarea' || tagName === 'select') {
    return true
  }
  const role = el.getAttribute ? el.getAttribute('role') : null
  return role === 'textbox'
}

const focusSearchInput = () => {
  if (!searchInput.value) return
  dropdown.value?.openMenu?.()
  if (typeof searchInput.value.focus === 'function') {
    try {
      searchInput.value.focus({ preventScroll: true })
    } catch (e) {
      searchInput.value.focus()
    }
  }
}

const handleGlobalSlash = (event) => {
  if (event.defaultPrevented) return
  if (event.key !== '/' || event.ctrlKey || event.metaKey || event.altKey) return
  if (isEditableElement(document.activeElement)) return
  event.preventDefault()
  focusSearchInput()
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalSlash)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalSlash)
})

const toggle = () => {
  dropdown.value.toggle()
}

const onClose = () => emit('close')

const fetchResults = async (kw) => {
  if (!kw) return []
  const res = await fetch(`${API_BASE_URL}/api/search/global?keyword=${encodeURIComponent(kw)}`)
  if (!res.ok) return []
  const data = await res.json()
  results.value = data.map((r) => ({
    id: r.id,
    text: r.text,
    type: r.type,
    subText: r.subText,
    extra: r.extra,
    postId: r.postId,
    highlightedText: r.highlightedText,
    highlightedSubText: r.highlightedSubText,
    highlightedExtra: r.highlightedExtra,
  }))
  return results.value
}

const escapeHtml = (value = '') =>
  String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const renderHighlight = (highlighted, fallback) => {
  if (highlighted) {
    return highlighted
  }
  const plain = stripMarkdown(fallback || '')
  if (!plain) {
    return ''
  }
  return escapeHtml(plain)
}

const iconMap = {
  user: 'UserIcon',
  post: 'FileText',
  post_title: 'FileText',
  comment: 'CommentIcon',
  category: 'Inbox',
  tag: 'TagOne',
}

watch(selected, (val) => {
  if (!val) return
  const opt = results.value.find((r) => r.id === val)
  if (!opt) return
  if (opt.type === 'post' || opt.type === 'post_title') {
    navigateTo(`/posts/${opt.id}`, { replace: true })
  } else if (opt.type === 'user') {
    navigateTo(`/users/${opt.id}`, { replace: true })
  } else if (opt.type === 'comment') {
    if (opt.postId) {
      navigateTo(`/posts/${opt.postId}#comment-${opt.id}`, { replace: true })
    }
  } else if (opt.type === 'category') {
    navigateTo({ path: '/', query: { category: opt.id } })
  } else if (opt.type === 'tag') {
    navigateTo({ path: '/', query: { tags: opt.id } })
  }
  selected.value = null
  keyword.value = ''
})

defineExpose({
  toggle,
})
</script>

<style scoped>
.search-dropdown {
  width: 300px;
}

.search-mobile-trigger {
  padding: 10px;
  font-size: 18px;
}

.search-input {
  padding: 2px 10px;
  display: flex;
  align-items: center;
  width: 100%;
}

.text-input {
  background-color: var(--app-menu-background-color);
  color: var(--text-color);
  border: none;
  outline: none;
  width: 100%;
  margin-left: 10px;
  font-size: 16px;
}

.search-menu {
  width: 100%;
  max-width: 600px;
}

@media (max-width: 768px) {
  .search-dropdown {
    width: 100%;
  }
}

.search-option-item {
  display: flex;
  gap: 10px;
}

.search-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
}

:deep(mark) {
  color: var(--primary-color);
}

.result-icon {
  opacity: 0.6;
}

.result-body {
  line-height: 1;
  display: flex;
  flex-direction: column;
}

.result-main {
  font-weight: bold;
}

.result-sub,
.result-extra {
  font-size: 12px;
  color: #666;
}

.search-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 10000;
}
</style>
