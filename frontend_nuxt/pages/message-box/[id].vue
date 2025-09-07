<template>
  <div class="chat-container" :class="{ float: isFloatMode }">
    <div v-if="!loading" class="chat-header">
      <div class="header-main">
        <div class="back-button" @click="goBack">
          <arrow-left />
        </div>
        <h2 class="participant-name">
          {{ isChannel ? conversationName : otherParticipant?.username }}
        </h2>
      </div>
      <div v-if="!isFloatMode" class="float-control">
        <collapse-text-input class="float-control-icon" @click="minimize" title="最小化" />
      </div>
    </div>

    <div class="messages-list" ref="messagesListEl">
      <div v-if="loading" class="loading-container">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <div v-else-if="error" class="error-container">{{ error }}</div>
      <template v-else>
        <div class="load-more-container" v-if="hasMoreMessages">
          <div @click="loadMoreMessages" :disabled="loadingMore" class="load-more-button">
            {{ loadingMore ? '加载中...' : '查看更多消息' }}
          </div>
        </div>
        <BaseTimeline :items="messages">
          <template #item="{ item }">
            <div class="message-header">
              <div class="user-name">
                {{ item.sender.username }}
              </div>
              <div class="message-timestamp">
                {{ TimeManager.format(item.createdAt) }}
              </div>
            </div>
            <div v-if="item.replyTo" class="reply-preview info-content-text">
              <div class="reply-author">{{ item.replyTo.sender.username }}</div>
              <div class="reply-content" v-html="renderMarkdown(item.replyTo.content)"></div>
            </div>
            <div class="message-content">
              <div class="info-content-text" v-html="renderMarkdown(item.content)"></div>
            </div>
            <ReactionsGroup
              :model-value="item.reactions"
              content-type="message"
              :content-id="item.id"
              @update:modelValue="(v) => (item.reactions = v)"
            >
              <div @click="setReply(item)" class="reply-btn"><next /> 写个回复...</div>
            </ReactionsGroup>
          </template>
        </BaseTimeline>
        <div class="empty-container">
          <BasePlaceholder
            v-if="messages.length === 0"
            text="暂无会话，发送消息试试 🎉"
            icon="inbox"
          />
        </div>
      </template>
    </div>

    <NewMessageContainer
      v-if="showNewMessageContainer"
      :count="newMessagesCount"
      :bottom="inputAreaHeight + 20"
      @click="handleNewMessagesClick"
    />

    <div class="message-input-area" ref="messageInputAreaEl">
      <div v-if="replyTo" class="active-reply">
        正在回复 {{ replyTo.sender.username }}:
        {{ stripMarkdownLength(replyTo.content, 50) }}
        <close-icon class="close-reply" @click="replyTo = null" />
      </div>
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
import { renderMarkdown, stripMarkdownLength } from '~/utils/markdown'
import MessageEditor from '~/components/MessageEditor.vue'
import ReactionsGroup from '~/components/ReactionsGroup.vue'
import { useWebSocket } from '~/composables/useWebSocket'
import { useUnreadCount } from '~/composables/useUnreadCount'
import { useChannelsUnreadCount } from '~/composables/useChannelsUnreadCount'
import TimeManager from '~/utils/time'
import BaseTimeline from '~/components/BaseTimeline.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import NewMessageContainer from '~/components/NewMessageContainer.vue'

const config = useRuntimeConfig()
const route = useRoute()
const API_BASE_URL = config.public.apiBaseUrl
const { connect, subscribe, unsubscribe, isConnected } = useWebSocket()
const { fetchUnreadCount: refreshGlobalUnreadCount } = useUnreadCount()
const { fetchChannelUnread: refreshChannelUnread } = useChannelsUnreadCount()

const messages = ref([])
const participants = ref([])
const loading = ref(true)
const sending = ref(false)
const error = ref(null)
const conversationId = route.params.id
const currentUser = ref(null)
const messagesListEl = ref(null)
const messageInputAreaEl = ref(null)
const currentPage = ref(0)
const totalPages = ref(0)
const loadingMore = ref(false)
const conversationName = ref('')
const isChannel = ref(false)
const isFloatMode = computed(() => route.query.float !== undefined)
const floatRoute = useState('messageFloatRoute')
const replyTo = ref(null)
const newMessagesCount = ref(0)
const inputAreaHeight = ref(0)
const showNewMessageContainer = computed(
  () => newMessagesCount.value > 0 && !isUserNearBottom.value,
)

function updateInputAreaHeight() {
  if (!messageInputAreaEl.value) return
  inputAreaHeight.value = messageInputAreaEl.value.offsetHeight
}

function handleNewMessagesClick() {
  scrollToBottomSmooth()
  newMessagesCount.value = 0
}

const isUserNearBottom = ref(true)
function updateNearBottom() {
  const el = messagesListEl.value
  if (!el) return
  const threshold = 40 // px
  isUserNearBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
}

const hasMoreMessages = computed(() => currentPage.value < totalPages.value - 1)

const otherParticipant = computed(() => {
  if (isChannel.value || !currentUser.value || participants.value.length === 0) {
    return null
  }
  return participants.value.find((p) => p.id !== currentUser.value.id)
})

function setReply(message) {
  replyTo.value = message
}

/** 改造：滚动函数 —— smooth & instant */
function scrollToBottomSmooth() {
  const el = messagesListEl.value
  if (!el) return
  // 优先使用原生 smooth，失败则降级
  try {
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  } catch {
    // 降级：简易动画
    const start = el.scrollTop
    const end = el.scrollHeight
    const duration = 200
    const startTs = performance.now()
    function step(now) {
      const p = Math.min(1, (now - startTs) / duration)
      el.scrollTop = start + (end - start) * p
      if (p < 1) requestAnimationFrame(step)
    }
    requestAnimationFrame(step)
  }
}

function scrollToBottomInstant() {
  const el = messagesListEl.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

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
      conversationName.value = conversationData.name
      isChannel.value = conversationData.channel
    }

    const newMessages = pageData.content.reverse().map((item) => ({
      ...item,
      src: item.sender.avatar,
      iconClick: () => {
        openUser(item.sender.id)
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

    await nextTick()
    if (page > 0 && list) {
      // 加载更多：保持原视口位置
      const newScrollHeight = list.scrollHeight
      list.scrollTop = newScrollHeight - oldScrollHeight
    } else if (page === 0) {
      // 首次加载：定位到底部（不用动画，避免“闪动感”）
      scrollToBottomInstant()
    }
    updateNearBottom()
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
  sending.value = true
  const token = getToken()
  try {
    let response
    if (isChannel.value) {
      response = await fetch(
        `${API_BASE_URL}/api/messages/conversations/${conversationId}/messages`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ content, replyToId: replyTo.value?.id }),
        },
      )
    } else {
      const recipient = otherParticipant.value
      if (!recipient) {
        toast.error('无法确定收信人')
        return
      }
      response = await fetch(`${API_BASE_URL}/api/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          recipientId: recipient.id,
          content: content,
          replyToId: replyTo.value?.id,
        }),
      })
    }
    if (!response.ok) throw new Error('发送失败')

    const newMessage = await response.json()
    messages.value.push({
      ...newMessage,
      src: newMessage.sender.avatar,
      iconClick: () => {
        openUser(newMessage.sender.id)
      },
    })
    clearInput()
    replyTo.value = null

    await nextTick()
    // 仅“发送消息成功后”才平滑滚动到底部
    scrollToBottomSmooth()
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
    refreshGlobalUnreadCount()
    refreshChannelUnread()
  } catch (e) {
    console.error('Failed to mark conversation as read', e)
  }
}

onMounted(async () => {
  // 监听列表滚动，实时感知是否接近底部
  if (messagesListEl.value) {
    messagesListEl.value.addEventListener('scroll', updateNearBottom, { passive: true })
  }

  window.addEventListener('resize', updateInputAreaHeight)
  await nextTick()
  updateInputAreaHeight()

  currentUser.value = await fetchCurrentUser()
  if (currentUser.value) {
    await fetchMessages(0)
    await markConversationAsRead()
    await nextTick()
    // 初次进入频道时，平滑滚动到底部
    scrollToBottomSmooth()
    const token = getToken()
    if (token) {
      if (isConnected.value) {
        subscribeToConversation()
      } else {
        connect(token)
      }
    }
  } else {
    toast.error('请先登录')
    loading.value = false
  }
})

const subscribeToConversation = () => {
  if (!currentUser.value) return
  const destination = `/topic/conversation/${conversationId}`

  subscribe(destination, async (message) => {
    try {
      const parsedMessage = JSON.parse(message.body)

      if (parsedMessage.sender && parsedMessage.sender.id === currentUser.value.id) {
        return
      }

      messages.value.push({
        ...parsedMessage,
        src: parsedMessage.sender.avatar,
        iconClick: () => openUser(parsedMessage.sender.id),
      })

      await markConversationAsRead()
      await nextTick()
      if (isUserNearBottom.value) {
        scrollToBottomSmooth()
      } else {
        newMessagesCount.value += 1
      }
    } catch (e) {
      console.error('Failed to parse websocket message', e)
    }
  })
}

watch(isConnected, (newValue) => {
  if (newValue) {
    subscribeToConversation()
  }
})

watch(isUserNearBottom, (val) => {
  if (val) newMessagesCount.value = 0
})

watch(replyTo, () => {
  nextTick(updateInputAreaHeight)
})

onActivated(async () => {
  // 返回页面时：刷新数据与已读，并滚动到底部
  if (currentUser.value) {
    await fetchMessages(0)
    await markConversationAsRead()
    await nextTick()
    scrollToBottomSmooth()
    updateNearBottom()

    if (isConnected.value) {
      // 如果已连接，重新订阅
      subscribeToConversation()
    } else {
      // 如果未连接，则发起连接
      const token = getToken()
      if (token) connect(token)
    }
  }
})

onDeactivated(() => {
  const destination = `/topic/conversation/${conversationId}`
  unsubscribe(destination)
})

onUnmounted(() => {
  const destination = `/topic/conversation/${conversationId}`
  unsubscribe(destination)

  if (messagesListEl.value) {
    messagesListEl.value.removeEventListener('scroll', updateNearBottom)
  }
  window.removeEventListener('resize', updateInputAreaHeight)
})

function minimize() {
  floatRoute.value = route.fullPath
  navigateTo('/')
}

function openUser(id) {
  if (isFloatMode.value) {
    // 先不处理...
    // navigateTo(`/users/${id}?float=1`)
  } else {
    navigateTo(`/users/${id}`, { replace: true })
  }
}

function goBack() {
  if (isFloatMode.value) {
    navigateTo('/message-box?float=1')
  } else {
    navigateTo('/message-box')
  }
}
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

.chat-container.float {
  height: 100vh;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid var(--normal-border-color);
  background-color: var(--background-color-blur);
  backdrop-filter: var(--blur-10);
}

.header-main {
  display: flex;
  align-items: center;
}

.float-control {
  position: absolute;
  top: 0;
  right: 0;
  text-align: right;
  padding: 12px 12px;
  cursor: pointer;
}

.float-control i {
  cursor: pointer;
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
  overflow-x: hidden;
  padding: 20px;
  padding-bottom: 100px;
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
}

.load-more-container {
  text-align: center;
}

.load-more-button {
  color: var(--primary-color);
  font-size: 12px;
  cursor: pointer;
}

.load-more-button:hover {
  text-decoration: underline;
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
  opacity: 0.6;
}

.message-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
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

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.error-container {
  text-align: center;
  padding: 50px;
  color: var(--text-color-secondary);
}

.message-input-area {
  margin-left: 10px;
  margin-right: 10px;
}

.reply-preview {
  margin-top: 10px;
  padding: 10px;
  border-left: 5px solid var(--primary-color);
  margin-bottom: 5px;
  font-size: 13px;
  background-color: var(--normal-light-background-color);
}

.reply-author {
  font-weight: bold;
  margin-bottom: 2px;
}

.reply-btn {
  cursor: pointer;
  padding: 4px;
  opacity: 0.6;
  font-size: 12px;
}

.reply-btn:hover {
  opacity: 1;
}

.active-reply {
  background-color: var(--normal-light-background-color);
  padding: 5px 10px;
  border-left: 5px solid var(--primary-color);
  margin-bottom: 5px;
  font-size: 13px;
}

.close-reply {
  margin-left: 8px;
  cursor: pointer;
}

@media (max-height: 200px) {
  .messages-list,
  .message-input-area {
    display: none;
  }
}

@media (max-width: 768px) {
  .messages-list {
    padding: 10px;
  }
}
</style>
