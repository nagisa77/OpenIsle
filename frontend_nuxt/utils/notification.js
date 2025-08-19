import { navigateTo, useRuntimeConfig } from 'nuxt/app'
import { reactive, ref } from 'vue'
import { toast } from '~/composables/useToast'
import { authState, getToken } from '~/utils/auth'
import { reactionEmojiMap } from '~/utils/reactions'

export const notificationState = reactive({
  unreadCount: 0,
})

const iconMap = {
  POST_VIEWED: 'fas fa-eye',
  COMMENT_REPLY: 'fas fa-reply',
  POST_REVIEWED: 'fas fa-shield-alt',
  POST_REVIEW_REQUEST: 'fas fa-gavel',
  POST_UPDATED: 'fas fa-comment-dots',
  USER_ACTIVITY: 'fas fa-user',
  FOLLOWED_POST: 'fas fa-feather-alt',
  USER_FOLLOWED: 'fas fa-user-plus',
  USER_UNFOLLOWED: 'fas fa-user-minus',
  POST_SUBSCRIBED: 'fas fa-bookmark',
  POST_UNSUBSCRIBED: 'fas fa-bookmark',
  REGISTER_REQUEST: 'fas fa-user-clock',
  ACTIVITY_REDEEM: 'fas fa-coffee',
  POINT_REDEEM: 'fas fa-gift',
  LOTTERY_WIN: 'fas fa-trophy',
  LOTTERY_DRAW: 'fas fa-bullhorn',
  MENTION: 'fas fa-at',
}

export async function fetchUnreadCount() {
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  try {
    const token = getToken()
    if (!token) {
      notificationState.unreadCount = 0
      return 0
    }
    const res = await fetch(`${API_BASE_URL}/api/notifications/unread-count`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!res.ok) {
      notificationState.unreadCount = 0
      return 0
    }
    const data = await res.json()
    notificationState.unreadCount = data.count
    return data.count
  } catch (e) {
    notificationState.unreadCount = 0
    return 0
  }
}

export async function markNotificationsRead(ids) {
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = config.public.apiBaseUrl

    const token = getToken()
    if (!token || !ids || ids.length === 0) return false
    const res = await fetch(`${API_BASE_URL}/api/notifications/read`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ids }),
    })
    return res.ok
  } catch (e) {
    return false
  }
}

export async function fetchNotificationPreferences() {
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = config.public.apiBaseUrl

    const token = getToken()
    if (!token) return []
    const res = await fetch(`${API_BASE_URL}/api/notifications/prefs`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (!res.ok) return []
    return await res.json()
  } catch (e) {
    return []
  }
}

export async function updateNotificationPreference(type, enabled) {
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = config.public.apiBaseUrl
    const token = getToken()
    if (!token) return false
    const res = await fetch(`${API_BASE_URL}/api/notifications/prefs`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ type, enabled }),
    })
    return res.ok
  } catch (e) {
    return false
  }
}

/**
 * 处理信息的高阶函数
 * @returns
 */
function createFetchNotifications() {
  const notifications = ref([])
  const isLoadingMessage = ref(false)
  const page = ref(0)
  const pageSize = 50
  const allLoaded = ref(false)
  const fetchNotifications = async (reset = false) => {
    const config = useRuntimeConfig()
    const API_BASE_URL = config.public.apiBaseUrl
    if (isLoadingMessage.value || (allLoaded.value && !reset)) return
    try {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      if (reset) {
        notifications.value = []
        page.value = 0
        allLoaded.value = false
      }
      isLoadingMessage.value = true
      const res = await fetch(
        `${API_BASE_URL}/api/notifications?page=${page.value}&pageSize=${pageSize}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        },
      )
      isLoadingMessage.value = false
      if (!res.ok) {
        toast.error('获取通知失败')
        return
      }
      const data = await res.json()

      for (const n of data) {
        if (n.type === 'COMMENT_REPLY') {
          notifications.value.push({
            ...n,
            src: n.comment.author.avatar,
            iconClick: () => {
              markRead(n.id)
              navigateTo(`/users/${n.comment.author.id}`, { replace: true })
            },
          })
        } else if (n.type === 'REACTION') {
          notifications.value.push({
            ...n,
            emoji: reactionEmojiMap[n.reactionType],
            iconClick: () => {
              if (n.fromUser) {
                markRead(n.id)
                navigateTo(`/users/${n.fromUser.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'POST_VIEWED') {
          notifications.value.push({
            ...n,
            src: n.fromUser ? n.fromUser.avatar : null,
            icon: n.fromUser ? undefined : iconMap[n.type],
            iconClick: () => {
              if (n.fromUser) {
                markRead(n.id)
                navigateTo(`/users/${n.fromUser.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'LOTTERY_WIN') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.post) {
                markRead(n.id)
                router.push(`/posts/${n.post.id}`)
              }
            },
          })
        } else if (n.type === 'LOTTERY_DRAW') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.post) {
                markRead(n.id)
                router.push(`/posts/${n.post.id}`)
              }
            },
          })
        } else if (n.type === 'POST_UPDATED') {
          notifications.value.push({
            ...n,
            src: n.comment.author.avatar,
            iconClick: () => {
              markRead(n.id)
              navigateTo(`/users/${n.comment.author.id}`, { replace: true })
            },
          })
        } else if (n.type === 'USER_ACTIVITY') {
          notifications.value.push({
            ...n,
            src: n.comment.author.avatar,
            iconClick: () => {
              markRead(n.id)
              navigateTo(`/users/${n.comment.author.id}`, { replace: true })
            },
          })
        } else if (n.type === 'MENTION') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.fromUser) {
                markRead(n.id)
                navigateTo(`/users/${n.fromUser.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'USER_FOLLOWED' || n.type === 'USER_UNFOLLOWED') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.fromUser) {
                markRead(n.id)
                navigateTo(`/users/${n.fromUser.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'FOLLOWED_POST') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.post) {
                markRead(n.id)
                navigateTo(`/posts/${n.post.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'POST_SUBSCRIBED' || n.type === 'POST_UNSUBSCRIBED') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {
              if (n.post) {
                markRead(n.id)
                navigateTo(`/posts/${n.post.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'POST_REVIEW_REQUEST') {
          notifications.value.push({
            ...n,
            src: n.fromUser ? n.fromUser.avatar : null,
            icon: n.fromUser ? undefined : iconMap[n.type],
            iconClick: () => {
              if (n.post) {
                markRead(n.id)
                navigateTo(`/posts/${n.post.id}`, { replace: true })
              }
            },
          })
        } else if (n.type === 'REGISTER_REQUEST') {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
            iconClick: () => {},
          })
        } else {
          notifications.value.push({
            ...n,
            icon: iconMap[n.type],
          })
        }
      }

      if (data.length < pageSize) {
        allLoaded.value = true
      } else {
        page.value++
      }
    } catch (e) {
      console.error(e)
    }
  }

  const markRead = async (id) => {
    if (!id) return
    const n = notifications.value.find((n) => n.id === id)
    if (!n || n.read) return
    n.read = true
    if (notificationState.unreadCount > 0) notificationState.unreadCount--
    const ok = await markNotificationsRead([id])
    if (!ok) {
      n.read = false
      notificationState.unreadCount++
    } else {
      fetchUnreadCount()
    }
  }

  const markAllRead = async () => {
    // 除了 REGISTER_REQUEST 类型消息
    const idsToMark = notifications.value
      .filter((n) => n.type !== 'REGISTER_REQUEST' && !n.read)
      .map((n) => n.id)
    if (idsToMark.length === 0) return
    notifications.value.forEach((n) => {
      if (n.type !== 'REGISTER_REQUEST') n.read = true
    })
    notificationState.unreadCount = notifications.value.filter((n) => !n.read).length
    const ok = await markNotificationsRead(idsToMark)
    if (!ok) {
      notifications.value.forEach((n) => {
        if (idsToMark.includes(n.id)) n.read = false
      })
      await fetchUnreadCount()
      return
    }
    fetchUnreadCount()
    if (authState.role === 'ADMIN') {
      toast.success('已读所有消息（注册请求除外）')
    } else {
      toast.success('已读所有消息')
    }
  }
  return {
    fetchNotifications,
    markRead,
    notifications,
    isLoadingMessage,
    markAllRead,
    allLoaded,
  }
}

export const {
  fetchNotifications,
  markRead,
  notifications,
  isLoadingMessage,
  markAllRead,
  allLoaded,
} = createFetchNotifications()
