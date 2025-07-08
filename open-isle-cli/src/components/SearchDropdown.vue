<template>
  <Dropdown
    v-model="selected"
    :fetch-options="fetchResults"
    remote
    menu-class="search-menu"
    option-class="search-option"
  >
    <template #display="{ toggle, setSearch }">
      <div class="search-input" @click="toggle">
        <i class="search-input-icon fas fa-search"></i>
        <input
          type="text"
          v-model="keyword"
          placeholder="Search"
          @focus="toggle"
          @input="setSearch(keyword)"
        />
      </div>
    </template>
    <template #option="{ option }">
      <i :class="['result-icon', iconMap[option.type] || 'fas fa-question']"></i>
      <div class="result-text">
        <div class="main" v-html="highlight(option.text)"></div>
        <div class="sub" v-if="option.subText" v-html="highlight(option.subText)"></div>
      </div>
    </template>
  </Dropdown>
</template>

<script>
import { ref } from 'vue'
import Dropdown from './Dropdown.vue'
import { API_BASE_URL } from '../main'

export default {
  name: 'SearchDropdown',
  components: { Dropdown },
  setup() {
    const keyword = ref('')
    const selected = ref(null)

    const fetchResults = async (kw) => {
      if (!kw) return []
      const res = await fetch(`${API_BASE_URL}/api/search/global?keyword=${encodeURIComponent(kw)}`)
      if (!res.ok) return []
      const data = await res.json()
      return data.map(r => ({
        id: r.id,
        text: r.text,
        subText: r.subText,
        type: r.type
      }))
    }

    const highlight = (text) => {
      if (!keyword.value) return text
      const reg = new RegExp(keyword.value, 'gi')
      return text.replace(reg, m => `<span class="highlight">${m}</span>`)
    }

    const iconMap = {
      user: 'fas fa-user',
      post: 'fas fa-file-alt',
      post_title: 'fas fa-file-alt',
      comment: 'fas fa-comment'
    }

    return { keyword, selected, fetchResults, highlight, iconMap }
  }
}
</script>

<style scoped>
.search-input {
  display: flex;
  align-items: center;
  border: 1px solid lightgray;
  border-radius: 10px;
  padding: 10px;
  width: 100%;
  max-width: 600px;
}
.search-input input {
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
.search-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
}
.result-text {
  display: flex;
  flex-direction: column;
}
.result-text .sub {
  font-size: 12px;
  color: gray;
}
.highlight {
  color: var(--primary-color);
}
.result-icon {
  opacity: 0.6;
}
</style>
