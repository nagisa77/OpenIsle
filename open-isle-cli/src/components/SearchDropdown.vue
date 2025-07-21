<template>
  <div class="search-dropdown">
    <Dropdown v-model="selected" :fetch-options="fetchResults" remote menu-class="search-menu"
      option-class="search-option" :show-search="false">
      <template #display="{ toggle, setSearch }">
        <div class="search-input" @click="toggle">
          <i class="search-input-icon fas fa-search"></i>
          <input class="text-input" v-model="keyword" placeholder="Search" @focus="toggle" @input="setSearch(keyword)" />
        </div>
      </template>
      <template #option="{ option }">
        <div class="search-option-item">
          <i :class="['result-icon', iconMap[option.type] || 'fas fa-question']"></i>
          <div class="result-body">
            <div class="result-main" v-html="highlight(option.text)"></div>
            <div v-if="option.subText" class="result-sub" v-html="highlight(option.subText)"></div>
            <div v-if="option.extra" class="result-extra" v-html="highlight(option.extra)"></div>
          </div>
        </div>
      </template>
    </Dropdown>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Dropdown from './Dropdown.vue'
import { API_BASE_URL } from '../main'
import { stripMarkdown } from '../utils/markdown'

export default {
  name: 'SearchDropdown',
  components: { Dropdown },
  emits: ['selected'],
  setup(props, { emit }) {
    const router = useRouter()
    const keyword = ref('')
    const selected = ref(null)
    const results = ref([])

    const fetchResults = async (kw) => {
      if (!kw) return []
      const res = await fetch(`${API_BASE_URL}/api/search/global?keyword=${encodeURIComponent(kw)}`)
      if (!res.ok) return []
      const data = await res.json()
      results.value = data.map(r => ({
        id: r.id,
        text: r.text,
        type: r.type,
        subText: r.subText,
        extra: r.extra,
        postId: r.postId
      }))
      return results.value
    }

    const highlight = (text) => {
      text = stripMarkdown(text)
      if (!keyword.value) return text
      const reg = new RegExp(keyword.value, 'gi')
      const res = text.replace(reg, m => `<span class="highlight">${m}</span>`)
      return res; 
    }

    const iconMap = {
      user: 'fas fa-user',
      post: 'fas fa-file-alt',
      comment: 'fas fa-comment'
    }

    watch(selected, val => {
      if (!val) return
      const opt = results.value.find(r => r.id === val)
      if (!opt) return
      if (opt.type === 'post' || opt.type === 'post_title') {
        router.push(`/posts/${opt.id}`)
      } else if (opt.type === 'user') {
        router.push(`/users/${opt.id}`)
      } else if (opt.type === 'comment') {
        if (opt.postId) {
          router.push(`/posts/${opt.postId}#comment-${opt.id}`)
        }
      }
      selected.value = null
      keyword.value = ''
      emit('selected')
    })

    return { keyword, selected, fetchResults, highlight, iconMap }
  }
}
</script>

<style scoped>
.search-dropdown {
  margin-top: 20px;
  width: 500px;
}

.search-input {
  padding: 10px;
  display: flex;
  align-items: center;
  width: 100%;
}

.text-input {
  background-color: var(--menu-background-color);
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

:deep(.highlight) {
  color: var(--primary-color);
}

.result-icon {
  opacity: 0.6;
}

.result-body {
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

@media (max-width: 768px) {
  .search-dropdown {
    width: 100%;
    margin-top: 0;
    padding: 10px;
  }

  .search-menu {
    max-width: none;
    width: 100%;
  }
}
</style>
