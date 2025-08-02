<template>
  <transition name="slide">
    <nav v-if="visible" class="menu">
      <div class="menu-item-container">
        <router-link
          class="menu-item"
          exact-active-class="selected"
          to="/"
          @click="handleHomeClick"
        >
          <i class="menu-item-icon fas fa-hashtag"></i>
          <span class="menu-item-text">话题</span>
        </router-link>
        <router-link
          class="menu-item"
          exact-active-class="selected"
          to="/message"
          @click="handleItemClick"
        >
          <i class="menu-item-icon fas fa-envelope"></i>
          <span class="menu-item-text">我的消息</span>
          <span v-if="unreadCount > 0" class="unread-container">
            <span class="unread"> {{ showUnreadCount }} </span>
          </span>
        </router-link>
        <router-link
          class="menu-item"
          exact-active-class="selected"
          to="/about"
          @click="handleItemClick"
        >
          <i class="menu-item-icon fas fa-info-circle"></i>
          <span class="menu-item-text">关于</span>
        </router-link>
        <router-link
          class="menu-item"
          exact-active-class="selected"
          to="/activities"
          @click="handleItemClick"
        >
          <i class="menu-item-icon fas fa-gift"></i>
          <span class="menu-item-text">🔥 活动</span>
        </router-link>
        <router-link
          v-if="shouldShowStats"
          class="menu-item"
          exact-active-class="selected"
          to="/about/stats"
          @click="handleItemClick"
        >
          <i class="menu-item-icon fas fa-chart-line"></i>
          <span class="menu-item-text">站点统计</span>
        </router-link>
        <router-link
          class="menu-item"
          exact-active-class="selected"
          to="/new-post"
          @click="handleItemClick"
        >
          <i class="menu-item-icon fas fa-edit"></i>
          <span class="menu-item-text">发帖</span>
        </router-link>
      </div>

      <div class="menu-section">
        <div class="section-header" @click="categoryOpen = !categoryOpen">
          <span>类别</span>
          <i :class="categoryOpen ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
        </div>
        <div v-if="categoryOpen" class="section-items">
          <div v-if="isLoadingCategory" class="menu-loading-container">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>
          <div v-else v-for="c in categories" :key="c.id" class="section-item" @click="gotoCategory(c)">
            <template v-if="c.smallIcon || c.icon">
              <img v-if="isImageIcon(c.smallIcon || c.icon)" :src="c.smallIcon || c.icon" class="section-item-icon" :alt="c.name" />
              <i v-else :class="['section-item-icon', c.smallIcon || c.icon]"></i>
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
          <span>tag</span>
          <i :class="tagOpen ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
        </div>
        <div v-if="tagOpen" class="section-items">
          <div v-if="isLoadingTag" class="menu-loading-container">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>
          <div v-else v-for="t in tags" :key="t.id" class="section-item" @click="gotoTag(t)">
            <img v-if="isImageIcon(t.smallIcon || t.icon)" :src="t.smallIcon || t.icon" class="section-item-icon" :alt="t.name" />
            <i v-else class="section-item-icon fas fa-hashtag"></i>
            <span class="section-item-text">{{ t.name }} <span class="section-item-text-count">x {{ t.count
                }}</span></span>
          </div>
        </div>
      </div>

      <div class="menu-footer">
        <div class="menu-footer-btn" @click="cycleTheme">
          <i :class="iconClass"></i>
        </div>
      </div>
    </nav>
  </transition>
</template>

<script>
import { themeState, cycleTheme, ThemeMode } from '../utils/theme'
import { authState } from '../utils/auth'
import { fetchUnreadCount, notificationState } from '../utils/notification'
import { watch } from 'vue'
import { API_BASE_URL } from '../main'
import { hatch } from 'ldrs'
hatch.register()

export default {
  name: 'MenuComponent',
  props: {
    visible: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      categories: [],
      tags: [],
      categoryOpen: true,
      tagOpen: true,
      isLoadingCategory: false,
      isLoadingTag: false
    }
  },
  computed: {
    iconClass() {
      switch (themeState.mode) {
        case ThemeMode.DARK:
          return 'fas fa-moon'
        case ThemeMode.LIGHT:
          return 'fas fa-sun'
        default:
          return 'fas fa-desktop'
      }
    },
    unreadCount() {
      return notificationState.unreadCount
    },
    showUnreadCount() {
      return this.unreadCount > 99 ? '99+' : this.unreadCount
    },
    shouldShowStats() {
      return authState.role === 'ADMIN'
    }
  },
  async mounted() {
    const updateCount = async () => {
      if (authState.loggedIn) {
        await fetchUnreadCount()
      } else {
        notificationState.unreadCount = 0
      }
    }
    
    watch(() => authState.loggedIn, async () => {
      await updateCount()
    })

    const CAT_CACHE_KEY = 'menu-categories'
    const TAG_CACHE_KEY = 'menu-tags'

    const cachedCategories = localStorage.getItem(CAT_CACHE_KEY)
    if (cachedCategories) {
      try {
        this.categories = JSON.parse(cachedCategories)
      } catch { /* ignore */ }
    }

    const cachedTags = localStorage.getItem(TAG_CACHE_KEY)
    if (cachedTags) {
      try {
        this.tags = JSON.parse(cachedTags)
      } catch { /* ignore */ }
    }

    this.isLoadingCategory = !cachedCategories
    this.isLoadingTag = !cachedTags

    const fetchCategories = () => {
      fetch(`${API_BASE_URL}/api/categories`).then(res => {
        if (res.ok) {
          res.json().then(data => {
            this.categories = data.slice(0, 10)
            localStorage.setItem(CAT_CACHE_KEY, JSON.stringify(this.categories))
          })
        }
        this.isLoadingCategory = false
      })
    }

    const fetchTags = () => {
      fetch(`${API_BASE_URL}/api/tags?limit=10`).then(res => {
        if (res.ok) {
          res.json().then(data => {
            this.tags = data
            localStorage.setItem(TAG_CACHE_KEY, JSON.stringify(this.tags))
          })
        }
        this.isLoadingTag = false
      })
    }

    if (cachedCategories) {
      setTimeout(fetchCategories, 1500)
    } else {
      fetchCategories()
    }

    if (cachedTags) {
      setTimeout(fetchTags, 1500)
    } else {
      fetchTags()
    }

    await updateCount()
  },
  methods: {
    cycleTheme,
    handleHomeClick() {
      this.$router.push('/').then(() => {
        window.location.reload()
      })
    },
    handleItemClick() {
      if (window.innerWidth <= 768) this.$emit('item-click')
    },
    isImageIcon(icon) {
      if (!icon) return false
      return /^https?:\/\//.test(icon) || icon.startsWith('/')
    },
    gotoCategory(c) {
      const value = encodeURIComponent(c.id ?? c.name)
      this.$router
        .push({ path: '/', query: { category: value } })
        .then(() => {
          window.location.reload()
        })
      this.handleItemClick()
    },
    gotoTag(t) {
      const value = encodeURIComponent(t.id ?? t.name)
      this.$router
        .push({ path: '/', query: { tags: value } })
        .then(() => {
          window.location.reload()
        })
      this.handleItemClick()
    }
  }
}
</script>

<style scoped>
.menu {
  width: 200px;
  background-color: var(--menu-background-color);
  height: calc(100vh - 10px);
  border-right: 1px solid var(--menu-border-color);
  display: flex;
  flex-direction: column;
  padding: 10px;
  overflow-y: auto;
  scrollbar-width: none;
  padding-top: calc(var(--header-height) + 10px);
}

.menu-item-container {
}

.menu-item {
  padding: 4px 10px;
  text-decoration: none;
  color: var(--menu-text-color);
  border-radius: 10px;
  display: flex;
  align-items: center;
}

.menu-item.selected {
  font-weight: bold;
  background-color: var(--menu-selected-background-color);
}

.menu-item-text {
  font-size: 16px;
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

.menu-item-icon {
  margin-right: 10px;
  opacity: 0.5;
  font-weight: bold;
}

.menu-footer {
  position: fixed;
  height: 30px;
  bottom: 10px;
  right: 10px;
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
}

.menu-section {
  margin-top: 10px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
  opacity: 0.5;
  padding: 4px 10px;
  cursor: pointer;
}

.section-items {
  color: var(--menu-text-color);
  display: flex;
  flex-direction: column;
  margin-top: 4px;
}

.section-item {
  padding: 4px 10px;
  display: flex;
  align-items: center;
  gap: 5px;
  border-radius: 8px;
  cursor: pointer;
}

.section-item:hover {
  background-color: var(--menu-selected-background-color);
}

.section-item-text-count {
  font-size: 12px;
  color: var(--menu-text-color);
  opacity: 0.5;
  font-weight: bold;
}

.section-item-text {
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
    backdrop-filter: blur(10px);
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
