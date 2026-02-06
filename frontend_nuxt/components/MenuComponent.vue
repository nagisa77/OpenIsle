<template>
  <transition name="slide">
    <nav v-if="visible" class="menu">
      <div class="menu-content">
        <div class="menu-item-container">
          <NuxtLink class="menu-item" exact-active-class="selected" to="/" @click="handleItemClick">
            <hashtag-key class="menu-item-icon" />
            <span class="menu-item-text">话题</span>
          </NuxtLink>
          <NuxtLink
            class="menu-item"
            exact-active-class="selected"
            to="/new-post"
            @click="handleItemClick"
          >
            <edit class="menu-item-icon" />
            <span class="menu-item-text">发帖</span>
          </NuxtLink>
          <NuxtLink
            class="menu-item"
            exact-active-class="selected"
            to="/message"
            @click="handleItemClick"
          >
            <remind class="menu-item-icon" />
            <span class="menu-item-text">我的消息</span>
            <span v-if="unreadCount > 0" class="unread-container">
              <span class="unread"> {{ showUnreadCount }} </span>
            </span>
          </NuxtLink>
          <NuxtLink
            class="menu-item"
            exact-active-class="selected"
            to="/about"
            @click="handleItemClick"
          >
            <info-icon class="menu-item-icon" />
            <span class="menu-item-text">关于</span>
          </NuxtLink>
          <NuxtLink
            class="menu-item"
            exact-active-class="selected"
            to="/activities"
            @click="handleItemClick"
          >
            <gift class="menu-item-icon" />
            <span class="menu-item-text">🔥 活动</span>
          </NuxtLink>
          <NuxtLink
            v-if="shouldShowStats"
            class="menu-item"
            exact-active-class="selected"
            to="/about/stats"
            @click="handleItemClick"
          >
            <chart-line class="menu-item-icon" />
            <span class="menu-item-text">站点统计</span>
          </NuxtLink>
          <NuxtLink
            v-if="authState.loggedIn"
            class="menu-item"
            exact-active-class="selected"
            to="/points"
            @click="handleItemClick"
          >
            <finance class="menu-item-icon" />
            <span class="menu-item-text">
              积分商城
              <span v-if="myPoint !== null" class="point-count">{{ myPoint }}</span>
            </span>
          </NuxtLink>
        </div>

        <div class="menu-section">
          <div class="section-header" @click="categoryOpen = !categoryOpen">
            <span>类别</span>
            <up v-if="categoryOpen" class="menu-item-icon" />
            <down v-else class="menu-item-icon" />
          </div>
          <div v-if="categoryOpen" class="section-items">
            <div v-if="isLoadingCategory" class="menu-loading-container">
              <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
            </div>
            <div
              v-else
              v-for="c in categoryData"
              :key="c.id"
              class="section-item"
              :class="{ selected: isCategorySelected(c.id) }"
              @click="gotoCategory(c)"
            >
              <template v-if="c.smallIcon || c.icon">
                <BaseImage
                  v-if="isImageIcon(c.smallIcon || c.icon)"
                  :src="c.smallIcon || c.icon"
                  class="section-item-icon"
                  :alt="c.name"
                />
                <component v-else :is="c.smallIcon || c.icon" class="section-item-icon" />
              </template>
              <span class="section-item-text">
                {{ c.name }}
                <span class="section-item-text-count" v-if="c.count >= 0">x {{ c.count }}</span>
              </span>
            </div>
          </div>
        </div>

        <div class="menu-section">
          <div class="section-header" @click="tagOpen = !tagOpen">
            <span>标签</span>
            <up v-if="tagOpen" class="menu-item-icon" />
            <down v-else class="menu-item-icon" />
          </div>
          <div v-if="tagOpen" class="section-items">
            <div v-if="isLoadingTag" class="menu-loading-container">
              <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
            </div>
            <template v-else>
              <div
                v-for="t in tagData"
                :key="t.id"
                class="section-item"
                :class="{ selected: isTagSelected(t.id) }"
                @click="gotoTag(t)"
              >
                <BaseImage
                  v-if="isImageIcon(t.smallIcon || t.icon)"
                  :src="t.smallIcon || t.icon"
                  class="section-item-icon"
                  :alt="t.name"
                />
                <component
                  v-else-if="t.smallIcon || t.icon"
                  :is="t.smallIcon || t.icon"
                  class="section-item-icon"
                />
                <tag-one v-else class="section-item-icon" />
                <span class="section-item-text"
                  >{{ t.name }} <span class="section-item-text-count">x {{ t.count }}</span></span
                >
              </div>
              <div v-if="hasMoreTags || isLoadingMoreTags" class="section-item more-item">
                <a
                  v-if="hasMoreTags && !isLoadingMoreTags"
                  href="#"
                  class="more-link"
                  @click.prevent="loadMoreTags"
                >
                  查看更多
                </a>
                <span v-else class="more-loading">加载中...</span>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 解决动态样式的水合错误 -->
      <ClientOnly v-if="!isMobile">
        <div class="menu-footer">
          <div class="menu-footer-btn" @click="cycleTheme">
            <component :is="iconClass" class="menu-item-icon" />
          </div>
        </div>
      </ClientOnly>
    </nav>
  </transition>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { authState, fetchCurrentUser } from '~/utils/auth'
import { fetchUnreadCount, notificationState } from '~/utils/notification'
import { useIsMobile } from '~/utils/screen'
import { cycleTheme, ThemeMode, themeState } from '~/utils/theme'
import { selectedCategoryGlobal, selectedTagsGlobal } from '~/composables/postFilter'

const isMobile = useIsMobile()
// 判断当前分类是否被选中
const isCategorySelected = (id) => {
  return id === selectedCategoryGlobal.value
}

// 判断当前标签是否被选中
const isTagSelected = (id) => {
  const selected = Array.isArray(selectedTagsGlobal.value)
    ? selectedTagsGlobal.value
    : [selectedTagsGlobal.value]
  return selected.includes(id)
}

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const props = defineProps({
  visible: { type: Boolean, default: true },
})
const emit = defineEmits(['item-click'])

const categoryOpen = ref(true)
const tagOpen = ref(true)
const myPoint = ref(null)

/** ✅ 用 useAsyncData 替换原生 fetch，避免 SSR+CSR 二次请求 */
const {
  data: categoryData,
  pending: isLoadingCategory,
  error: categoryError,
} = await useAsyncData(
  // 稳定 key：避免 hydration 期误判
  'menu:categories',
  () => $fetch(`${API_BASE_URL}/api/categories`),
  {
    server: true, // SSR 预取
    default: () => [], // 初始默认值，减少空判断
    // 5 分钟内复用缓存，避免路由往返重复请求
    staleTime: 5 * 60 * 1000,
  },
)

const TAG_PAGE_SIZE = 10
const tagPage = ref(0)
const hasMoreTags = ref(true)
const isLoadingMoreTags = ref(false)

const buildTagUrl = (page = 0) => {
  const base = API_BASE_URL || (import.meta.client ? window.location.origin : '')
  const url = new URL('/api/tags', base)
  url.searchParams.set('page', String(page))
  url.searchParams.set('pageSize', String(TAG_PAGE_SIZE))
  return url.toString()
}

const fetchTagPage = async (page = 0) => {
  try {
    return await $fetch(buildTagUrl(page))
  } catch (e) {
    console.error('Failed to fetch tags', e)
    return []
  }
}

const {
  data: tagData,
  pending: isLoadingTag,
  error: tagError,
} = await useAsyncData('menu:tags', () => fetchTagPage(0), {
  server: true,
  default: () => [],
  staleTime: 5 * 60 * 1000,
})

const dedupeTags = (list) => Array.from(new Map(list.map((tag) => [tag.id, tag])).values())

const initializeTagState = (val) => {
  const initial = Array.isArray(val) ? val : []
  if (!Array.isArray(val)) {
    tagData.value = []
  }
  tagPage.value = 0
  hasMoreTags.value = initial.length === TAG_PAGE_SIZE
}

initializeTagState(tagData.value)

watch(
  tagData,
  (val, oldVal) => {
    const next = Array.isArray(val) ? val : []
    if (!Array.isArray(val)) {
      tagData.value = []
    }
    const shouldReset =
      !Array.isArray(oldVal) || oldVal.length > next.length || next.length <= TAG_PAGE_SIZE
    if (shouldReset) {
      tagPage.value = 0
      hasMoreTags.value = next.length === TAG_PAGE_SIZE
    }
  },
  { deep: false },
)

const loadMoreTags = async () => {
  if (isLoadingMoreTags.value || !hasMoreTags.value) return
  isLoadingMoreTags.value = true
  const nextPage = tagPage.value + 1
  try {
    const result = await fetchTagPage(nextPage)
    const data = Array.isArray(result) ? result : []
    const existing = Array.isArray(tagData.value) ? tagData.value : []
    tagData.value = dedupeTags([...existing, ...data])
    tagPage.value = nextPage
    if (data.length < TAG_PAGE_SIZE) {
      hasMoreTags.value = false
    }
  } catch (e) {
    console.error('Failed to load more tags', e)
  } finally {
    isLoadingMoreTags.value = false
  }
}

/** 其余逻辑保持不变 */
const iconClass = computed(() => {
  switch (themeState.mode) {
    case ThemeMode.DARK:
      return 'Moon'
    case ThemeMode.LIGHT:
      return 'SunOne'
    default:
      return 'ComputerOne'
  }
})

const unreadCount = computed(() => notificationState.unreadCount)
const showUnreadCount = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))
const shouldShowStats = computed(() => authState.role === 'ADMIN')

const loadPoint = async () => {
  if (authState.loggedIn) {
    const user = await fetchCurrentUser()
    myPoint.value = user ? user.point : null
  } else {
    myPoint.value = null
  }
}

const updateCount = async () => {
  if (authState.loggedIn) {
    await fetchUnreadCount()
  } else {
    notificationState.unreadCount = 0
  }
}

onMounted(async () => {
  await Promise.all([updateCount(), loadPoint()])
  // 登录态变化时再拉一次未读数和积分；与 useAsyncData 无关
  watch(
    () => authState.loggedIn,
    () => {
      updateCount()
      loadPoint()
    },
  )
})

const handleItemClick = () => {
  if (window.innerWidth <= 768) emit('item-click')
}

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}

const gotoCategory = (c) => {
  const value = encodeURIComponent(c.id ?? c.name)
  navigateTo({ path: '/', query: { category: value } })
  handleItemClick()
}

const gotoTag = (t) => {
  const value = encodeURIComponent(t.id ?? t.name)
  navigateTo({ path: '/', query: { tags: value } })
  handleItemClick()
}
</script>

<style scoped>
.menu {
  position: sticky;
  top: var(--header-height);
  width: 220px;
  background-color: var(--app-menu-background-color);
  height: calc(100vh - var(--header-height));
  border-right: 1px solid var(--menu-border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
  backdrop-filter: var(--blur-10);
}

.menu-content {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 10px 10px 0 10px;
}

.menu-content::-webkit-scrollbar {
  display: none;
}

.menu-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.menu-item-container {
  border-bottom: 1px solid var(--menu-border-color);
}

.menu-item:last-child {
  margin-bottom: 5px;
}

/* .menu-item-container { */
/**/
/* } */

.menu-item {
  padding: 6px 12px;
  text-decoration: none;
  color: var(--menu-text-color);
  border-radius: 10px;
  display: flex;
  align-items: center;
  transition: background-color 0.5s ease;
}

.menu-item:hover {
  background-color: var(--menu-selected-background-color-hover);
}

.menu-item.selected {
  font-weight: bold;
  background-color: var(--menu-selected-background-color);
}

.menu-item-text {
  font-size: 14px;
  text-decoration: none;
  color: var(--menu-text-color);
}

.unread-container {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: rgb(255, 102, 102);
  margin-left: 15px;
  width: 18px;
  height: 18px;
  text-align: center;
}

.unread {
  color: white;
  font-size: 9px;
  font-weight: bold;
}

.point-count {
  margin-left: 4px;
  font-size: 12px;
  color: var(--primary-color);
}

.menu-item-icon {
  margin-right: 10px;
  opacity: 0.5;
  font-weight: bold;
}

.menu-footer {
  position: relation;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.menu-footer-btn {
  width: 30px;
  height: 30px;
  margin-right: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.menu-section {
  border-bottom: 1px solid var(--menu-border-color);
  padding-bottom: 5px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding: 6px 12px 0 12px;
  color: var(--menu-text-color);
  cursor: pointer;
}

.section-items {
  color: var(--menu-text-color);
  display: flex;
  flex-direction: column;
  margin-top: 4px;
}

.section-item {
  padding: 6px 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.5s ease;
}

.more-item {
  justify-content: center;
}

.more-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
}

.more-link:hover {
  text-decoration: underline;
}

.more-loading {
  font-size: 13px;
  color: var(--menu-text-color);
  opacity: 0.7;
}

.section-item:hover {
  background-color: var(--menu-selected-background-color-hover);
}
.section-item.selected {
  font-weight: bold;
  background-color: var(--menu-selected-background-color);
}

.section-item-text-count {
  font-size: 12px;
  color: var(--menu-text-color);
  opacity: 0.5;
  font-weight: bold;
}

.section-item-text {
  font-size: 14px;
  text-decoration: none;
  color: var(--menu-text-color);
}

.section-item-icon {
  width: 16px;
  height: 16px;
  margin-right: 5px;
  opacity: 0.7;
}

.menu-loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

@media (max-width: 768px) {
  .menu {
    position: fixed;
    z-index: 1000;
    box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
    left: 10px;
    border-radius: 20px;
    border-right: none;
    height: 400px;
    top: calc(var(--header-height) + 10px);
    padding-top: 10px;
    background-color: var(--background-color-blur);
  }

  .menu-content {
    border-radius: 20px;
  }

  .slide-enter-active,
  .slide-leave-active {
    transition:
      transform 0.3s ease,
      opacity 0.3s ease,
      width 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateX(-100%);
    opacity: 0;
  }

  .slide-enter-to,
  .slide-leave-from {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
