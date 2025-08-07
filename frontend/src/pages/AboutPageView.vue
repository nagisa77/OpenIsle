<template>
  <div class="about-page">
    <div class="about-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        :class="['about-tabs-item', { selected: selectedTab === tab.name }]"
        @click="selectTab(tab.name)"
      >
        <div class="about-tabs-item-label">{{ tab.label }}</div>
      </div>
    </div>
    <div class="about-content" v-html="renderMarkdown(content)" @click="handleContentClick"></div>
    <div class="about-loading" v-if="isFetching">
      <l-hatch-spinner size="100" stroke="10" speed="1" color="var(--primary-color)" />
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { renderMarkdown, handleMarkdownClick } from '../utils/markdown'
import { hatch } from 'ldrs'
hatch.register()

export default {
  name: 'AboutPageView',
  setup() {
    const isFetching = ref(false)
    const tabs = [
      { name: 'about', label: '关于', file: '/about_markdown/about.md' },
      { name: 'agreement', label: '用户协议', file: '/about_markdown/agreement.md' },
      { name: 'guideline', label: '创作准则', file: '/about_markdown/guideline.md' },
      { name: 'privacy', label: '隐私政策', file: '/about_markdown/privacy.md' },
    ]
    const selectedTab = ref(tabs[0].name)
    const content = ref('')

    const loadContent = async (file) => {
      try {
        isFetching.value = true
        const res = await fetch(file)
        if (res.ok) {
          content.value = await res.text()
        } else {
          content.value = '# 内容加载失败'
        }
      } catch (e) {
        content.value = '# 内容加载失败'
      }
      isFetching.value = false
    }

    const selectTab = (name) => {
      selectedTab.value = name
      const tab = tabs.find(t => t.name === name)
      if (tab) loadContent(tab.file)
    }

    onMounted(() => {
      loadContent(tabs[0].file)
    })

    const handleContentClick = e => {
      handleMarkdownClick(e)
    }

    return { tabs, selectedTab, content, renderMarkdown, selectTab, isFetching, handleContentClick }
  }
}
</script>

<style scoped>
.about-page {
  max-width: var(--page-max-width);
  background-color: var(--background-color);
  margin: 0 auto;
}

.about-tabs {
  position: sticky;
  top: calc(var(--header-height) + 1px);
  z-index: 200;
  background-color: var(--background-color-blur);
  display: flex;
  flex-direction: row;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 20px;
  overflow-x: auto;
  scrollbar-width: none;
}

.about-tabs-item {
  padding: 10px 20px;
  cursor: pointer;
  white-space: nowrap;
}

.about-tabs-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.about-content {
  line-height: 1.6;
  padding: 20px;
}

.about-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

@media (max-width: 768px) {
  .about-tabs {
    width: 100vw;
  }
}

</style>
