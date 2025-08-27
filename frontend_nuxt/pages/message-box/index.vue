<template>
  <div class="messages-container">
    <div class="page-title">
      <i class="fas fa-comments"></i>
      <span class="page-title-text">选择聊天</span>
    </div>
    <div v-if="!isFloatMode" class="float-control">
      <i class="fas fa-compress" @click="minimize" title="最小化"></i>
    </div>
    <BaseTabs v-model="activeTab" :tabs="tabs">
      <template #default>
        <div v-if="activeTab === 'messages'">
          <div v-if="loading" class="loading-message">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>

          <div v-else-if="error" class="error-container">
            <div class="error-text">{{ error }}</div>
          </div>

          <div v-if="!loading && !isFloatMode" class="search-container">
            <SearchPersonDropdown />
          </div>

          <div v-if="!loading && conversations.length === 0" class="empty-container">
            <BasePlaceholder
              v-if="conversations.length === 0"
              text="暂无会话"
              icon="fas fa-inbox"
            />
          </div>

          <div
            v-if="!loading"
            v-for="convo in conversations"
            :key="convo.id"
            class="conversation-item"
            @click="goToConversation(convo.id)"
          >
            <div class="conversation-avatar">
              <img
                :src="getOtherParticipant(convo)?.avatar || '/default-avatar.svg'"
                :alt="getOtherParticipant(convo)?.username || '用户'"
                class="avatar-img"
                @error="handleAvatarError"
              />
            </div>

            <div class="conversation-content">
              <div class="conversation-header">
                <div class="participant-name">
                  {{ getOtherParticipant(convo)?.username || '未知用户' }}
                </div>
                <div class="message-time">
                  {{ formatTime(convo.lastMessage?.createdAt || convo.createdAt) }}
                </div>
              </div>

              <div class="last-message-row">
                <div class="last-message">
                  {{
                    convo.lastMessage
                      ? stripMarkdownLength(convo.lastMessage.content, 100)
                      : '暂无消息'
                  }}
                </div>
                <div v-if="convo.unreadCount > 0" class="unread-count-badge">
                  {{ convo.unreadCount }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else>
          <div v-if="loadingChannels" class="loading-message">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>
          <div v-else>
            <div v-if="channels.length === 0" class="empty-container">
              <BasePlaceholder text="暂无频道" icon="fas fa-inbox" />
            </div>
            <div
              v-for="ch in channels"
              :key="ch.id"
              class="conversation-item"
              @click="goToChannel(ch.id)"
            >
              <div class="conversation-avatar">
                <img
                  :src="ch.avatar || '/default-avatar.svg'"
                  :alt="ch.name"
                  class="avatar-img"
                  @error="handleAvatarError"
                />
              </div>
              <div class="conversation-content">
                <div class="conversation-header">
                  <div class="participant-name">
                    {{ ch.name }}
                    <span v-if="ch.unreadCount > 0" class="unread-dot"></span>
                  </div>
                  <div class="message-time">
                    {{ formatTime(ch.lastMessage?.createdAt || ch.createdAt) }}
                  </div>
                </div>
                <div class="last-message-row">
                  <div class="last-message">
                    {{
                      ch.lastMessage
                        ? stripMarkdownLength(ch.lastMessage.content, 100)
                        : ch.description
                    }}
                  </div>
                  <div class="member-count">成员 {{ ch.memberCount }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </BaseTabs>
  </div>
</template>

<script setup>
import { ref, onUnmounted, watch, onActivated, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getToken, fetchCurrentUser } from '~/utils/auth'
import { toast } from '~/main'
import { useWebSocket } from '~/composables/useWebSocket'
import { useUnreadCount } from '~/composables/useUnreadCount'
import { useChannelsUnreadCount } from '~/composables/useChannelsUnreadCount'
import TimeManager from '~/utils/time'
import { stripMarkdownLength } from '~/utils/markdown'
import SearchPersonDropdown from '~/components/SearchPersonDropdown.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import BaseTabs from '~/components/BaseTabs.vue'

const config = useRuntimeConfig()
const conversations = ref([])
const loading = ref(true)
const error = ref(null)

const route = useRoute()
const currentUser = ref(null)
const API_BASE_URL = config.public.apiBaseUrl
const { connect, disconnect, subscribe, isConnected } = useWebSocket()
const { fetchUnreadCount: refreshGlobalUnreadCount } = useUnreadCount()
const { fetchChannelUnread: refreshChannelUnread, setFromList: setChannelUnreadFromList } =
  useChannelsUnreadCount()
let subscription = null

const activeTab = ref('channels')
const channels = ref([])
const loadingChannels = ref(false)
const isFloatMode = computed(() => route.query.float === '1')
const floatRoute = useState('messageFloatRoute')
const tabs = [
  { name: 'messages', label: '站内信' },
  { name: 'channels', label: '频道' },
]

async function fetchConversations() {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/messages/conversations`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    const data = await response.json()
    conversations.value = data
  } catch (e) {
    error.value = '无法加载会话列表。'
  } finally {
    loading.value = false
  }
}

// 获取对话中的另一个参与者（非当前用户）
function getOtherParticipant(conversation) {
  if (!currentUser.value || !conversation.participants) return null
  return conversation.participants.find((p) => p.id !== currentUser.value.id)
}

// 格式化时间
function formatTime(timeString) {
  if (!timeString) return ''
  return TimeManager.format(timeString)
}

// 头像加载失败处理
function handleAvatarError(event) {
  event.target.src = '/default-avatar.svg'
}

async function fetchChannels() {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  loadingChannels.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/channels`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!response.ok) throw new Error('无法加载频道')
    const data = await response.json()
    channels.value = data
    setChannelUnreadFromList(data)
  } catch (e) {
    toast.error(e.message)
  } finally {
    loadingChannels.value = false
  }
}

async function goToChannel(id) {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    await fetch(`${API_BASE_URL}/api/channels/${id}/join`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (isFloatMode.value) {
      navigateTo(`/message-box/${id}?float=1`)
    } else {
      navigateTo(`/message-box/${id}`)
    }
  } catch (e) {
    toast.error(e.message)
  }
}

onActivated(async () => {
  currentUser.value = await fetchCurrentUser()

  if (currentUser.value) {
    if (activeTab.value === 'messages') {
      await fetchConversations()
    } else {
      await fetchChannels()
    }
    refreshGlobalUnreadCount()
    refreshChannelUnread()
    const token = getToken()
    if (token && !isConnected.value) {
      connect(token)
    }
  } else {
    loading.value = false
  }
})

watch(isConnected, (newValue) => {
  if (newValue && currentUser.value) {
    const destination = `/topic/user/${currentUser.value.id}/messages`

    // 清理旧的订阅
    if (subscription) {
      subscription.unsubscribe()
    }

    subscription = subscribe(destination, (message) => {
      fetchConversations()
      if (activeTab.value === 'channels') {
        fetchChannels()
      }
    })
  }
})

watch(activeTab, (val) => {
  if (val === 'messages') {
    fetchConversations()
  } else {
    fetchChannels()
  }
})

onUnmounted(() => {
  if (subscription) {
    subscription.unsubscribe()
  }
  disconnect()
})

function goToConversation(id) {
  if (isFloatMode.value) {
    navigateTo(`/message-box/${id}?float=1`)
  } else {
    navigateTo(`/message-box/${id}`)
  }
}

function minimize() {
  floatRoute.value = route.fullPath
  navigateTo('/')
}
</script>

<style scoped>
.messages-container {
  position: relative;
}

.float-control {
  position: absolute;
  top: 0;
  right: 0;
  text-align: right;
  padding: 12px 12px;
}

.float-control i {
  cursor: pointer;
}

.messages-container :deep(.base-tabs-header) {
  margin-bottom: 16px;
}

.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.search-container {
  margin-bottom: 24px;
  margin-left: 20px;
  margin-right: 20px;
}

.messages-header {
  margin-bottom: 24px;
}

.page-title {
  padding: 12px;
  display: none;
  flex-direction: row;
  gap: 10px;
}

.page-title-text {
  margin-left: 10px;
}

.page-title-text:hover {
  text-decoration: underline;
}

.messages-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.loading-text,
.error-text,
.empty-text {
  font-size: 16px;
  color: #666;
}

.error-text {
  color: #e53e3e;
}

.conversations-list {
}

.conversation-item {
  display: flex;
  align-items: center;
  margin-left: 20px;
  margin-right: 20px;
  padding: 8px 10px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.conversation-item:hover {
  background-color: var(--menu-selected-background-color);
}

.conversation-avatar {
  flex-shrink: 0;
  margin-right: 12px;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.participant-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
}

.member-count {
  font-size: 12px;
  color: gray;
  flex-shrink: 0;
}

.message-time {
  font-size: 12px;
  color: gray;
  flex-shrink: 0;
  margin-left: 12px;
}

.last-message-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-message {
  font-size: 14px;
  color: gray;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-grow: 1;
  padding-right: 10px; /* Add some space between message and badge */
}

.unread-count-badge {
  background-color: #f56c6c;
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 12px;
  line-height: 1.5;
  flex-shrink: 0;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background-color: #f56c6c;
  border-radius: 50%;
  margin-left: 4px;
}

@media (max-height: 200px) {
  .page-title {
    display: block;
  }

  .base-tabs-wrapper,
  .loading-message,
  .error-container,
  .search-container,
  .empty-container,
  .conversation-item {
    display: none;
  }
}

@media (max-width: 768px) {
  .conversation-item {
    margin-left: 10px;
    margin-right: 10px;
  }

  .messages-title {
    font-size: 24px;
  }

  .conversations-list {
    max-height: 500px;
  }

  .conversation-item {
    padding: 6px 8px;
  }

  .avatar-img {
    width: 40px;
    height: 40px;
  }

  .participant-name {
    font-size: 15px;
  }

  .message-time {
    font-size: 11px;
  }

  .last-message {
    font-size: 13px;
  }
}
</style>
