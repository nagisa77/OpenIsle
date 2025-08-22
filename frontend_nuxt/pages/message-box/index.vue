<template>
  <div class="messages-container">
    <div class="tabs">
      <div
        class="tab"
        :class="{ active: activeTab === 'messages' }"
        @click="activeTab = 'messages'"
      >
        站内信
      </div>
      <div
        class="tab"
        :class="{ active: activeTab === 'channels' }"
        @click="activeTab = 'channels'"
      >
        频道
      </div>
    </div>

    <div v-if="activeTab === 'messages'">
      <div v-if="loading" class="loading-message">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>

      <div v-else-if="error" class="error-container">
        <div class="error-text">{{ error }}</div>
      </div>

      <div v-else-if="conversations.length === 0" class="empty-container">
        <div class="empty-text">暂无会话</div>
      </div>

      <div
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
                convo.lastMessage ? stripMarkdownLength(convo.lastMessage.content, 100) : '暂无消息'
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
      <div v-if="channelsLoading" class="loading-message">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <div v-else-if="channelsError" class="error-container">
        <div class="error-text">{{ channelsError }}</div>
      </div>
      <div v-else-if="channels.length === 0" class="empty-container">
        <div class="empty-text">暂无频道</div>
      </div>
      <div
        v-for="channel in channels"
        :key="channel.id"
        class="conversation-item"
        @click="goToChannel(channel)"
      >
        <div class="conversation-avatar" style="position: relative">
          <img
            :src="channel.avatar || '/default-avatar.svg'"
            :alt="channel.name"
            class="avatar-img"
            @error="handleAvatarError"
          />
          <span v-if="channel.unreadCount > 0" class="unread-dot"></span>
        </div>
        <div class="conversation-content">
          <div class="conversation-header">
            <div class="participant-name">{{ channel.name }}</div>
          </div>
          <div class="last-message-row">
            <div class="last-message">{{ channel.description }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted, watch, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getToken, fetchCurrentUser } from '~/utils/auth'
import { toast } from '~/main'
import { useWebSocket } from '~/composables/useWebSocket'
import { useUnreadCount } from '~/composables/useUnreadCount'
import TimeManager from '~/utils/time'
import { stripMarkdownLength } from '~/utils/markdown'

const config = useRuntimeConfig()
const activeTab = ref('messages')
const conversations = ref([])
const loading = ref(true)
const error = ref(null)
const channels = ref([])
const channelsLoading = ref(true)
const channelsError = ref(null)
const router = useRouter()
const currentUser = ref(null)
const API_BASE_URL = config.public.apiBaseUrl
const { connect, disconnect, subscribe, isConnected } = useWebSocket()
const { fetchUnreadCount: refreshGlobalUnreadCount } = useUnreadCount()
let subscription = null

async function fetchConversations() {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const response = await fetch(`${API_BASE_URL}/api/messages/conversations`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    const data = await response.json()
    conversations.value = data.filter((c) => !c.channel)
  } catch (e) {
    error.value = '无法加载会话列表。'
  } finally {
    loading.value = false
  }
}

async function fetchChannels() {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const response = await fetch(`${API_BASE_URL}/api/channels`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    channels.value = await response.json()
  } catch (e) {
    channelsError.value = '无法加载频道。'
  } finally {
    channelsLoading.value = false
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

onActivated(async () => {
  loading.value = true
  currentUser.value = await fetchCurrentUser()

  if (currentUser.value) {
    await fetchConversations()
    await fetchChannels()
    refreshGlobalUnreadCount() // Refresh global count when entering the list
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
      fetchChannels()
    })
  }
})

onUnmounted(() => {
  if (subscription) {
    subscription.unsubscribe()
  }
  disconnect()
})

function goToConversation(id) {
  router.push(`/message-box/${id}`)
}

async function goToChannel(channel) {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  await fetch(`${API_BASE_URL}/api/channels/${channel.id}/join`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  router.push(`/message-box/${channel.conversationId}`)
}
</script>

<style scoped>
.messages-container {
  margin: 0 auto;
  padding: 20px;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 10px;
}

.tab {
  padding: 8px 16px;
  cursor: pointer;
}

.tab.active {
  border-bottom: 2px solid var(--primary-color);
  font-weight: 600;
}

.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.messages-header {
  margin-bottom: 24px;
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
  position: absolute;
  top: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background-color: #f56c6c;
  border-radius: 50%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .messages-container {
    padding: 10px 10px;
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
