<template>
  <header class="header">
    <div class="header-content">
      <div class="header-content-left">
        <div v-if="showMenuBtn" class="menu-btn-wrapper">
          <button class="menu-btn" ref="menuBtn" @click="$emit('toggle-menu')">
            <i class="fas fa-bars"></i>
          </button>
          <span
            v-if="isMobile && (unreadMessageCount > 0 || hasChannelUnread)"
            class="menu-unread-dot"
          ></span>
        </div>
        <NuxtLink class="logo-container" :to="`/`" @click="refrechData">
          <img
            alt="OpenIsle"
            src="https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png"
            width="60"
            height="60"
          />
          <div class="logo-text">OpenIsle</div>
        </NuxtLink>
      </div>

      <ClientOnly>
        <div class="header-content-right">
          <div v-if="isMobile" class="search-icon" @click="search">
            <i class="fas fa-search"></i>
          </div>

          <div v-if="isMobile" class="theme-icon" @click="cycleTheme">
            <i :class="iconClass"></i>
          </div>

          <div v-if="!isMobile" class="invite_text" @click="copyInviteLink">
            <i class="fas fa-copy"></i>
            邀请
            <i v-if="isCopying" class="fas fa-spinner fa-spin"></i>
          </div>

          <ToolTip content="复制RSS链接" placement="bottom">
            <div class="rss-icon" @click="copyRssLink">
              <i class="fas fa-rss"></i>
            </div>
          </ToolTip>

          <ToolTip v-if="!isMobile && isLogin" content="发帖" placement="bottom">
            <div class="new-post-icon" @click="goToNewPost">
              <i class="fas fa-edit"></i>
            </div>
          </ToolTip>

          <ToolTip v-if="isLogin" content="站内信和频道" placement="bottom">
            <div class="messages-icon" @click="goToMessages">
              <i class="fas fa-comments"></i>
              <span v-if="unreadMessageCount > 0" class="unread-badge">{{
                unreadMessageCount
              }}</span>
              <span v-else-if="hasChannelUnread" class="unread-dot"></span>
            </div>
          </ToolTip>

          <DropdownMenu v-if="isLogin" ref="userMenu" :items="headerMenuItems">
            <template #trigger>
              <div class="avatar-container">
                <BaseImg class="avatar-img" :src="avatar" alt="avatar" width="32" height="32" />
                <i class="fas fa-caret-down dropdown-icon"></i>
              </div>
            </template>
          </DropdownMenu>

          <div v-if="!isLogin" class="auth-btns">
            <div class="header-content-item-main" @click="goToLogin">登录</div>
            <div class="header-content-item-secondary" @click="goToSignup">注册</div>
          </div>
        </div>
      </ClientOnly>

      <SearchDropdown ref="searchDropdown" v-if="isMobile && showSearch" @close="closeSearch" />
    </div>
  </header>
</template>

<script setup>
import { ClientOnly } from '#components'
import { computed, nextTick, ref, watch } from 'vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import ToolTip from '~/components/ToolTip.vue'
import SearchDropdown from '~/components/SearchDropdown.vue'
import BaseImg from '~/components/BaseImg.vue'
import { authState, clearToken, loadCurrentUser } from '~/utils/auth'
import { useUnreadCount } from '~/composables/useUnreadCount'
import { useChannelsUnreadCount } from '~/composables/useChannelsUnreadCount'
import { useIsMobile } from '~/utils/screen'
import { themeState, cycleTheme, ThemeMode } from '~/utils/theme'
import { toast } from '~/main'
import { getToken } from '~/utils/auth'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const WEBSITE_BASE_URL = config.public.websiteBaseUrl

const props = defineProps({
  showMenuBtn: {
    type: Boolean,
    default: true,
  },
})

const isLogin = computed(() => authState.loggedIn)
const isMobile = useIsMobile()
const { count: unreadMessageCount, fetchUnreadCount } = useUnreadCount()
const { hasUnread: hasChannelUnread, fetchChannelUnread } = useChannelsUnreadCount()
const avatar = ref('')
const showSearch = ref(false)
const searchDropdown = ref(null)
const userMenu = ref(null)
const menuBtn = ref(null)
const isCopying = ref(false)

const search = () => {
  showSearch.value = true
  nextTick(() => {
    searchDropdown.value.toggle()
  })
}
const closeSearch = () => {
  nextTick(() => {
    showSearch.value = false
  })
}
const goToLogin = () => {
  navigateTo('/login', { replace: true })
}
const goToSettings = () => {
  navigateTo('/settings', { replace: true })
}

const copyInviteLink = async () => {
  isCopying.value = true
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const res = await fetch(`${API_BASE_URL}/api/invite/generate`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok) {
      const data = await res.json()
      const inviteLink = data.token ? `${WEBSITE_BASE_URL}/signup?invite_token=${data.token}` : ''
      /**
       * navigator.clipboard在webkit中有点奇怪的行为
       * https://stackoverflow.com/questions/62327358/javascript-clipboard-api-safari-ios-notallowederror-message
       * https://webkit.org/blog/10247/new-webkit-features-in-safari-13-1/
       */
      setTimeout(() => {
        navigator.clipboard
          .writeText(inviteLink)
          .then(() => {
            toast.success('邀请链接已复制')
          })
          .catch(() => {
            toast.error('邀请链接复制失败')
          })
      }, 0)
    } else {
      const data = await res.json().catch(() => ({}))
      toast.error(data.error || '生成邀请链接失败')
    }
  } catch (e) {
    toast.error('生成邀请链接失败')
  } finally {
    isCopying.value = false
  }
}

const copyRssLink = async () => {
  const rssLink = `${API_BASE_URL}/api/rss`
  await navigator.clipboard.writeText(rssLink)
  toast.success('RSS链接已复制')
}

const goToProfile = async () => {
  if (!authState.loggedIn) {
    navigateTo('/login', { replace: true })
    return
  }
  let id = authState.username || authState.userId
  if (!id) {
    const user = await loadCurrentUser()
    if (user) {
      id = user.username || user.id
    }
  }
  if (id) {
    navigateTo(`/users/${id}`, { replace: true })
  }
}
const goToSignup = () => {
  navigateTo('/signup', { replace: true })
}
const goToLogout = () => {
  clearToken()
  navigateTo('/login', { replace: true })
}

const goToNewPost = () => {
  navigateTo('/new-post', { replace: false })
}

const refrechData = async () => {
  window.dispatchEvent(new Event('refresh-home'))
}

const goToMessages = () => {
  navigateTo('/message-box')
}

const headerMenuItems = computed(() => [
  { text: '设置', onClick: goToSettings },
  { text: '个人主页', onClick: goToProfile },
  { text: '退出', onClick: goToLogout },
])

/** 其余逻辑保持不变 */
const iconClass = computed(() => {
  switch (themeState.mode) {
    case ThemeMode.DARK:
      return 'fas fa-moon'
    case ThemeMode.LIGHT:
      return 'fas fa-sun'
    default:
      return 'fas fa-desktop'
  }
})

onMounted(async () => {
  const updateAvatar = async () => {
    if (authState.loggedIn) {
      const user = await loadCurrentUser()
      if (user && user.avatar) {
        avatar.value = user.avatar
      }
    }
  }
  const updateUnread = async () => {
    if (authState.loggedIn) {
      fetchUnreadCount()
      fetchChannelUnread()
    } else {
      fetchChannelUnread()
    }
  }

  await updateAvatar()
  await updateUnread()

  watch(
    () => authState.loggedIn,
    async (isLoggedIn) => {
      await updateAvatar()
      await updateUnread()
    },
  )
})
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--header-height);
  background-color: var(--background-color-blur);
  backdrop-filter: var(--blur-10);
  color: var(--header-text-color);
  border-bottom: 1px solid var(--header-border-color);
}

.logo-container {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
}

.header-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  width: 100%;
  height: 100%;
  max-width: var(--page-max-width);
  backdrop-filter: var(--blur-10);
}

.header-content-left {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.header-content-right {
  display: flex;
  margin-left: auto;
  flex-direction: row;
  align-items: center;
  gap: 30px;
}

.auth-btns {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20px;
}

.menu-btn {
  font-size: 24px;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  opacity: 0.4;
  margin-right: 10px;
}

.menu-btn-wrapper {
  position: relative;
  display: inline-block;
}

.menu-unread-dot {
  position: absolute;
  top: 0;
  right: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.menu-btn:hover {
  opacity: 0.8;
}

.header-content-item-main {
  background-color: var(--primary-color);
  color: white;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
}

.header-content-item-main:hover {
  background-color: var(--primary-color-hover);
}

.header-content-item-secondary {
  color: var(--primary-color);
  text-decoration: underline;
  cursor: pointer;
}

.avatar-container {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: lightgray;
  object-fit: cover;
}

.dropdown-icon {
  margin-left: 5px;
}

.dropdown-item {
  padding: 8px 16px;
  white-space: nowrap;
}

.dropdown-item:hover {
  background-color: var(--menu-selected-background-color);
}

.search-icon,
.theme-icon {
  font-size: 18px;
  cursor: pointer;
}

.invite_text {
  font-size: 12px;
  cursor: pointer;
  color: var(--primary-color);
}

.invite_text:hover {
  text-decoration: underline;
}

.rss-icon,
.new-post-icon,
.messages-icon {
  font-size: 18px;
  cursor: pointer;
  position: relative;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -10px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  padding: 2px 5px;
  font-size: 10px;
  font-weight: bold;
  line-height: 1;
  min-width: 16px;
  text-align: center;
  box-sizing: border-box;
}

.unread-dot {
  position: absolute;
  top: -2px;
  right: -4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.rss-icon {
  animation: rss-glow 2s 3;
}

@keyframes rss-glow {
  0% {
    text-shadow: 0 0 0px var(--primary-color);
    opacity: 1;
  }
  50% {
    text-shadow: 0 0 12px var(--primary-color);
    opacity: 0.8;
  }
  100% {
    text-shadow: 0 0 0px var(--primary-color);
    opacity: 1;
  }
}

@media (max-width: 1200px) {
  .header-content {
    padding-left: 15px;
    padding-right: 15px;
    width: calc(100% - 30px);
  }
}

@media (max-width: 768px) {
  .header-content-item-secondary {
    display: none;
  }

  .logo-text {
    display: none;
  }

  .header-content-right {
    gap: 15px;
  }
}
</style>
