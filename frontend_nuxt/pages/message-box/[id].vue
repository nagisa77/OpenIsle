<template>
  <div class="chat-container">
    <div v-if="!loading" class="chat-header">
      <NuxtLink to="/message-box" class="back-button">
        <i class="fas fa-arrow-left"></i>
      </NuxtLink>
      <h2 class="participant-name">{{ channel ? channel.name : otherParticipant?.username }}</h2>
    </div>

    <div class="messages-list" ref="messagesListEl">
      <div v-if="loading" class="loading-container">加载中...</div>
      <div v-else-if="error" class="error-container">{{ error }}</div>
      <template v-else>
        <div class="load-more-container" v-if="hasMoreMessages">
          <button @click="loadMoreMessages" :disabled="loadingMore" class="load-more-button">
            {{ loadingMore ? '加载中...' : '查看更多消息' }}
          </button>
        </div>
        <BaseTimeline :items="messages">
          <template #item="{ item }">
            <div class="message-timestamp">
              {{ TimeManager.format(item.createdAt) }}
            </div>
            <div class="message-content">
              <div class="info-content-text" v-html="renderMarkdown(item.content)"></div>
            </div>
          </template>
        </BaseTimeline>
      </template>
    </div>

    <div class="message-input-area">
      <MessageEditor :loading="sending" @submit="sendMessage" />
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  onMounted,
  onUnmounted,
  nextTick,
  computed,
  watch,
  onActivated,
  onDeactivated,
} from 'vue'
import { useRoute } from 'vue-router'
import { getToken, fetchCurrentUser } from '~/utils/auth'
import { toast } from '~/main'
import { renderMarkdown } from '~/utils/markdown'
import MessageEditor from '~/components/MessageEditor.vue'
import { useWebSocket } from '~/composables/useWebSocket'
import { useUnreadCount } from '~/composables/useUnreadCount'
import TimeManager from '~/utils/time'
import BaseTimeline from '~/components/BaseTimeline.vue'

const config = useRuntimeConfig()
const route = useRoute()
const API_BASE_URL = config.public.apiBaseUrl
const { connect, disconnect, subscribe, isConnected } = useWebSocket()
const { fetchUnreadCount: refreshGlobalUnreadCount } = useUnreadCount()
let subscription = null

const messages = ref([])
const participants = ref([])
const channel = ref(null)
const loading = ref(true)
const sending = ref(false)
const error = ref(null)
const conversationId = route.params.id
const currentUser = ref(null)
const messagesListEl = ref(null)
let lastMessageEl = null
const currentPage = ref(0)
const totalPages = ref(0)
const loadingMore = ref(false)
let scrollInterval = null

const hasMoreMessages = computed(() => currentPage.value < totalPages.value - 1)

const otherParticipant = computed(() => {
  if (!currentUser.value || participants.value.length === 0) {
    return null
  }
  return participants.value.find((p) => p.id !== currentUser.value.id)
})

function isSentByCurrentUser(message) {
  return message.sender.id === currentUser.value?.id
}

function handleAvatarError(event) {
  event.target.src = '/default-avatar.svg'
}

// No changes needed here, as renderMarkdown is now imported.
// The old function is removed.

async function fetchMessages(page = 0) {
  if (page === 0) {
    loading.value = true
    messages.value = []
  } else {
    loadingMore.value = true
  }
  error.value = null
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    loading.value = false
    return
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/api/messages/conversations/${conversationId}?page=${page}&size=20`,
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    )
    if (!response.ok) throw new Error('无法加载消息')

    const conversationData = await response.json()
    const pageData = conversationData.messages

    if (page === 0) {
      participants.value = conversationData.participants
      channel.value = conversationData.channel
    }

    // Since the backend sorts by descending, we need to reverse for correct chat order
    const newMessages = pageData.content.reverse().map((item) => ({
      ...item,
      src: item.sender.avatar,
      iconClick: () => {
        navigateTo(`/users/${item.sender.id}`, { replace: true })
      },
    }))

    const list = messagesListEl.value
    const oldScrollHeight = list ? list.scrollHeight : 0

    if (page === 0) {
      messages.value = newMessages
    } else {
      messages.value = [...newMessages, ...messages.value]
    }

    currentPage.value = pageData.number
    totalPages.value = pageData.totalPages

    // Scrolling is now fully handled by the watcher
    await nextTick()
    if (page > 0 && list) {
      const newScrollHeight = list.scrollHeight
      list.scrollTop = newScrollHeight - oldScrollHeight
    }
  } catch (e) {
    error.value = e.message
    toast.error(e.message)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMoreMessages() {
  if (hasMoreMessages.value && !loadingMore.value) {
    await fetchMessages(currentPage.value + 1)
  }
}

async function sendMessage(content, clearInput) {
  if (!content.trim()) return

  const token = getToken()
  sending.value = true
  try {
    let url
    let body
    if (channel.value) {
      url = `${API_BASE_URL}/api/messages/conversations/${conversationId}/messages`
      body = { content: content }
    } else {
      const recipient = otherParticipant.value
      if (!recipient) {
        toast.error('无法确定收信人')
        sending.value = false
        return
      }
      url = `${API_BASE_URL}/api/messages`
      body = { recipientId: recipient.id, content: content }
    }
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    })
    if (!response.ok) throw new Error('发送失败')

    const newMessage = await response.json()
    messages.value.push({
      ...newMessage,
      src: newMessage.sender.avatar,
      iconClick: () => {
        navigateTo(`/users/${newMessage.sender.id}`, { replace: true })
      },
    })
    clearInput()
    setTimeout(() => {
      scrollToBottom()
    }, 100)
  } catch (e) {
    toast.error(e.message)
  } finally {
    sending.value = false
  }
}

async function markConversationAsRead() {
  const token = getToken()
  if (!token) return
  try {
    await fetch(`${API_BASE_URL}/api/messages/conversations/${conversationId}/read`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    })
    // After marking as read, refresh the global unread count
    refreshGlobalUnreadCount()
  } catch (e) {
    console.error('Failed to mark conversation as read', e)
  }
}

function scrollToBottom() {
  if (messagesListEl.value) {
    const element = messagesListEl.value
    // 強制滾動到底部，使用 smooth 行為確保視覺效果
    element.scrollTop = element.scrollHeight

    // 再次確認滾動位置
    setTimeout(() => {
      if (element.scrollTop < element.scrollHeight - element.clientHeight) {
        element.scrollTop = element.scrollHeight
      }
    }, 50)
  }
}

watch(
  messages,
  async (newMessages) => {
    if (newMessages.length === 0) return

    await nextTick()

    // Simple, reliable scroll to bottom
    setTimeout(() => {
      scrollToBottom()
    }, 100)
  },
  { deep: true },
)

onMounted(async () => {
  currentUser.value = await fetchCurrentUser()
  if (currentUser.value) {
    await fetchMessages(0)
    await markConversationAsRead()
    const token = getToken()
    if (token && !isConnected.value) {
      connect(token)
    }
  } else {
    toast.error('请先登录')
    loading.value = false
  }
})

watch(isConnected, (newValue) => {
  if (newValue) {
    // 等待一小段时间确保连接稳定
    setTimeout(() => {
      subscription = subscribe(`/topic/conversation/${conversationId}`, (message) => {
        // 避免重复显示当前用户发送的消息
        if (message.sender.id !== currentUser.value.id) {
          messages.value.push({
            ...message,
            src: message.sender.avatar,
            iconClick: () => {
              navigateTo(`/users/${message.sender.id}`, { replace: true })
            },
          })
          // 实时收到消息时自动标记为已读
          markConversationAsRead()
          setTimeout(() => {
            scrollToBottom()
          }, 100)
        }
      })
    }, 500)
  }
})

onActivated(async () => {
  // This will be called every time the component is activated (navigated to)
  if (currentUser.value) {
    await fetchMessages(0)
    await markConversationAsRead()

    // 確保滾動到底部 - 使用多重延遲策略
    await nextTick()
    setTimeout(() => {
      scrollToBottom()
    }, 100)
    setTimeout(() => {
      scrollToBottom()
    }, 300)
    setTimeout(() => {
      scrollToBottom()
    }, 500)

    if (!isConnected.value) {
      const token = getToken()
      if (token) connect(token)
    }
  }
})

onDeactivated(() => {
  if (subscription) {
    subscription.unsubscribe()
    subscription = null
  }
  disconnect()
})

onUnmounted(() => {
  if (subscription) {
    subscription.unsubscribe()
    subscription = null
  }
  disconnect()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  overflow: auto;
  height: calc(100vh - var(--header-height));
  position: relative;
}

.chat-header {
  display: flex;
  position: sticky;
  top: 0;
  z-index: 100;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid var(--normal-border-color);
  background-color: var(--background-color-blur);
  backdrop-filter: var(--blur-10);
}

.back-button {
  font-size: 18px;
  color: var(--text-color-primary);
  margin-right: 15px;
  cursor: pointer;
}

.participant-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.messages-list {
  overflow-y: auto;
  padding: 20px;
  padding-bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 10px;
}

.load-more-container {
  text-align: center;
  margin-bottom: 20px;
}

.load-more-button {
  background-color: var(--bg-color-soft);
  border: 1px solid var(--border-color);
  color: var(--text-color-primary);
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.load-more-button:hover {
  background-color: var(--border-color);
}

.message-item {
  display: flex;
  gap: 10px;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  align-self: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-timestamp {
  font-size: 11px;
  color: var(--text-color-secondary);
  margin-top: 5px;
  opacity: 0.6;
}

.message-item.sent {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-item.sent .message-timestamp {
  text-align: right;
}

/* Received messages */
.message-item.received {
  align-self: flex-start;
}

.message-item.received .message-content {
  align-items: flex-start;
}

.message-item.received .message-bubble {
  background-color: var(--bg-color-soft);
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 4px;
}

.message-input-area {
  margin-left: 20px;
  margin-right: 20px;
}

.loading-container,
.error-container {
  text-align: center;
  padding: 50px;
  color: var(--text-color-secondary);
}

@media (max-width: 768px) {
  .messages-list {
    padding: 10px;
  }
}

.message-input-area {
  margin-left: 10px;
  margin-right: 10px;
}
</style>
