<template>
  <header class="header">
    <div class="header-content">
      <div class="header-content-left">
        <div v-if="showMenuBtn" class="menu-btn-wrapper">
          <button class="menu-btn" ref="menuBtn" @click="$emit('toggle-menu')">
            <ToolTip content="展开/收起菜单" placement="bottom">
              <application-menu class="micon"></application-menu>
            </ToolTip>
          </button>
          <span
            v-if="isMobile && (unreadMessageCount > 0 || hasChannelUnread)"
            class="menu-unread-dot"
          ></span>
        </div>
        <NuxtLink class="logo-container" :to="`/`" @click="refrechData">
          <BaseImage
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
          <SearchDropdown
            ref="searchDropdown"
            v-if="!isMobile || showSearch"
            @close="closeSearch"
          />
          <!-- 搜索 -->
          <ToolTip v-if="isMobile" content="搜索" placement="bottom">
            <div class="header-icon-item" @click="search">
              <search-icon class="header-icon" />
              <span class="header-label">搜索</span>
            </div>
          </ToolTip>
          <!-- 主题切换 -->
          <ToolTip v-if="isMobile" content="切换主题" placement="bottom">
            <div class="header-icon-item" @click="cycleTheme">
              <component :is="iconClass" class="header-icon" />
              <span class="header-label">主题</span>
            </div>
          </ToolTip>
          <!-- 邀请 -->
          <ToolTip v-if="!isMobile" content="邀请好友" placement="bottom">
            <div class="header-icon-item" @click="copyInviteLink">
              <template v-if="!isCopying">
                <copy-link class="header-icon" />
                <span class="header-label">邀请</span>
              </template>
              <loading v-else />
            </div>
          </ToolTip>
          <!-- 在线人数 -->
          <ToolTip v-if="!isMobile" content="当前在线人数" placement="bottom">
            <div class="header-icon-item">
              <peoples-two class="header-icon" />
              <span class="header-label">在线</span>
              <span class="header-badge">{{ onlineCount }}</span>
            </div>
          </ToolTip>
          <!-- RSS -->
          <ToolTip content="复制RSS链接" placement="bottom">
            <div class="header-icon-item" @click="copyRssLink">
              <rss class="header-icon" />
              <span class="header-label">RSS</span>
            </div>
          </ToolTip>
          <!-- 发帖 -->
          <ToolTip v-if="!isMobile && isLogin" content="发帖" placement="bottom">
            <div class="header-icon-item" @click="goToNewPost">
              <edit class="header-icon" />
              <span class="header-label">发帖</span>
            </div>
          </ToolTip>

          <!-- 消息 -->
          <ToolTip v-if="isLogin" content="站内信和频道" placement="bottom">
            <div class="header-icon-item" @click="goToMessages">
              <message-emoji class="header-icon" />
              <span class="header-label">消息</span>
              <span v-if="unreadMessageCount > 0" class="unread-badge">{{
                unreadMessageCount
              }}</span>
              <span v-else-if="hasChannelUnread" class="unread-dot"></span>
            </div>
          </ToolTip>

          <DropdownMenu v-if="isLogin" ref="userMenu" :items="headerMenuItems">
            <template #trigger>
              <div class="avatar-container">
                <BaseUserAvatar
                  class="avatar-img"
                  :user-id="authState.userId"
                  :src="authState.avatar"
                  :disable-link="true"
                  :width="32"
                />
                <down />
              </div>
            </template>
          </DropdownMenu>

          <div v-if="!isLogin" class="auth-btns">
            <div class="header-content-item-main" @click="goToLogin">登录</div>
            <div class="header-content-item-secondary" @click="goToSignup">注册</div>
          </div>
        </div>
      </ClientOnly>
    </div>
  </header>
</template>

<script setup>
import { ClientOnly } from '#components'
import { computed, nextTick, ref, watch } from 'vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import ToolTip from '~/components/ToolTip.vue'
import SearchDropdown from '~/components/SearchDropdown.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import { authState, clearToken } from '~/utils/auth'
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
const showSearch = ref(false)
const searchDropdown = ref(null)
const userMenu = ref(null)
const menuBtn = ref(null)
const isCopying = ref(false)
const onlineCount = ref(0)

// 心跳检测
async function sendPing() {
  try {
    // 已登录就用 userId，否则随机生成游客ID
    let userId = authState.userId
    if (userId) {
      // 用户已登录，清理游客 ID
      localStorage.removeItem('guestId')
    } else {
      // 游客模式
      let savedId = localStorage.getItem('guestId')
      if (!savedId) {
        savedId = `guest-${crypto.randomUUID()}`
        localStorage.setItem('guestId', savedId)
      }
      userId = savedId
    }
    const res = await fetch(`${API_BASE_URL}/api/online/heartbeat?userId=${userId}`, {
      method: 'POST',
    })
  } catch (e) {
    console.error('心跳失败', e)
  }
}

// 获取在线人数
async function fetchCount() {
  try {
    const res = await fetch(`${API_BASE_URL}/api/online/count`, {
      method: 'GET',
    })
    onlineCount.value = await res.json()
  } catch (e) {
    console.error('获取在线人数失败', e)
  }
}

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
    isCopying.value = false // 🔥 修复：未登录时立即复原状态
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
  let id = authState.username || authState.id
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
      return 'Moon'
    case ThemeMode.LIGHT:
      return 'SunOne'
    default:
      return 'ComputerOne'
  }
})

onMounted(async () => {
  const updateUnread = async () => {
    if (authState.loggedIn) {
      fetchUnreadCount()
      fetchChannelUnread()
    } else {
      fetchChannelUnread()
    }
  }

  await updateUnread()

  // 新增的在线人数逻辑
  sendPing()
  fetchCount()
  setInterval(sendPing, 120000) // 每 2 分钟发一次心跳
  setInterval(fetchCount, 60000) // 每 1 分更新 UI
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
  color: var(--primary-color);
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
  padding-right: 15px;
}

.micon {
  margin-left: 10px;
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
  margin-right: 10px;
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

.invite_text:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.invite_text,
.online-count,
.rss-icon,
.new-post-icon,
.messages-icon {
  font-size: 18px;
  cursor: pointer;
  position: relative;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -6px;
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
  top: 0;
  right: -1px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.rss-icon {
  animation: rss-glow 2s 3;
}

.online-count {
  cursor: default;
}

/* === 统一图标按钮风格 === */
.header-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  color: var(--primary-color);
  cursor: pointer;
  position: relative;
  transition:
    color 0.25s ease,
    transform 0.15s ease,
    opacity 0.2s ease;
}

.header-icon-item:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

/* 点击时瞬间高亮 + 轻微缩放 */
.header-icon-item:active {
  color: var(--primary-color-hover);
  transform: scale(0.92);
}

.header-icon {
  font-size: 20px;
  line-height: 1;
}

.header-label {
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

/* 在线人数的数字文字样式（无背景） */
.header-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  color: var(--primary-color); /* 🔹 使用主题主色 */
  background: none; /* 🔹 去掉背景 */
  font-size: 11px; /* 字体稍微大一点以便清晰 */
  font-weight: 600; /* 加一点权重让数字更醒目 */
  line-height: 1;
  padding: 0; /* 去掉内边距 */
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
  /* 手机不显示文字 */
  .header-label {
    display: none;
  }
  .header-badge {
    display: none;
  }
}
</style>
