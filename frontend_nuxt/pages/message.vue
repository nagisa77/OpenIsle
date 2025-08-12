<template>
  <div class="message-page">
    <div class="message-page-header">
      <div class="message-tabs">
        <div
          :class="['message-tab-item', { selected: selectedTab === 'all' }]"
          @click="selectedTab = 'all'"
        >
          消息
        </div>
        <div
          :class="['message-tab-item', { selected: selectedTab === 'unread' }]"
          @click="selectedTab = 'unread'"
        >
          未读
        </div>
        <div
          :class="['message-tab-item', { selected: selectedTab === 'control' }]"
          @click="selectedTab = 'control'"
        >
          消息设置
        </div>
      </div>

      <div class="message-page-header-right">
        <div class="message-page-header-right-item" @click="markAllRead">
          <i class="fas fa-bolt message-page-header-right-item-button-icon"></i>
          <span class="message-page-header-right-item-button-text"> 已读所有消息 </span>
        </div>
      </div>
    </div>

    <div v-if="selectedTab === 'control'">
      <div class="message-control-container">
        <div class="message-control-title">通知设置</div>
        <div class="message-control-push-item-container">
          <div
            v-for="pref in notificationPrefs"
            :key="pref.type"
            class="message-control-push-item"
            :class="{ select: pref.enabled }"
            @click="togglePref(pref)"
          >
            {{ formatType(pref.type) }}
          </div>
        </div>
      </div>
    </div>

    <template v-else>
      <div v-if="isLoadingMessage" class="loading-message">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>

      <BasePlaceholder
        v-else-if="filteredNotifications.length === 0"
        text="暂时没有消息 :)"
        icon="fas fa-inbox"
      />

      <div class="timeline-container" v-if="filteredNotifications.length > 0">
        <BaseTimeline :items="filteredNotifications">
          <template #item="{ item }">
            <div class="notif-content" :class="{ read: item.read }">
              <span v-if="!item.read" class="unread-dot"></span>
              <span class="notif-type">
                <template v-if="item.type === 'COMMENT_REPLY' && item.parentComment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.comment.author.id}`"
                      >{{ item.comment.author.username }}
                    </router-link>
                    对我的评论
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.parentComment.id}`"
                      >
                        {{ stripMarkdownLength(item.parentComment.content, 100) }}
                      </router-link>
                    </span>
                    回复了
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        {{ stripMarkdownLength(item.comment.content, 100) }}
                      </router-link>
                    </span>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'COMMENT_REPLY' && !item.parentComment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.comment.author.id}`"
                      >{{ item.comment.author.username }}
                    </router-link>
                    对我的文章
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </router-link>
                    </span>
                    回复了
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        {{ stripMarkdownLength(item.comment.content, 100) }}
                      </router-link>
                    </span>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'ACTIVITY_REDEEM' && !item.parentComment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <span class="notif-user">{{ item.fromUser.username }} </span>
                    申请进行奶茶兑换，联系方式是：{{ item.content }}
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'REACTION' && item.post && !item.comment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <span class="notif-user">{{ item.fromUser.username }} </span> 对我的文章
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}`"
                      >
                        {{ stripMarkdownLength(item.post.title, 100) }}
                      </router-link>
                    </span>
                    进行了表态
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'REACTION' && item.comment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                      >{{ item.fromUser.username }}
                    </router-link>
                    对我的评论
                    <span>
                      <router-link
                        class="notif-content-text"
                        @click="markRead(item.id)"
                        :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                      >
                        {{ stripMarkdownLength(item.comment.content, 100) }}
                      </router-link>
                    </span>
                    进行了表态
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_VIEWED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    查看了您的帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_UPDATED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    您关注的帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    下面有新评论
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                    >
                      {{ stripMarkdownLength(item.comment.content, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'USER_ACTIVITY' && item.parentComment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    你关注的
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.comment.author.id}`"
                    >
                      {{ item.comment.author.username }}
                    </router-link>
                    在 对评论
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}#comment-${item.parentComment.id}`"
                    >
                      {{ stripMarkdownLength(item.parentComment.content, 100) }}
                    </router-link>
                    回复了
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                    >
                      {{ stripMarkdownLength(item.comment.content, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'USER_ACTIVITY'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    你关注的
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.comment.author.id}`"
                    >
                      {{ item.comment.author.username }}
                    </router-link>
                    在文章
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    下面评论了
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                    >
                      {{ stripMarkdownLength(item.comment.content, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'MENTION' && item.comment">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    在评论中提到了你：
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}#comment-${item.comment.id}`"
                    >
                      {{ stripMarkdownLength(item.comment.content, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'MENTION'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    在帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    中提到了你
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'USER_FOLLOWED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    开始关注你了
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'USER_UNFOLLOWED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    取消关注你了
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'FOLLOWED_POST'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    你关注的
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    发布了文章
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_SUBSCRIBED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    订阅了你的文章
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_UNSUBSCRIBED'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    取消订阅了你的文章
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_REVIEW_REQUEST' && item.fromUser">
                  <NotificationContainer :item="item" :markRead="markRead">
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/users/${item.fromUser.id}`"
                    >
                      {{ item.fromUser.username }}
                    </router-link>
                    发布了帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    ，请审核
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_REVIEW_REQUEST'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    您发布的帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    已提交审核
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'REGISTER_REQUEST'">
                  <NotificationContainer :item="item" :markRead="markRead">
                    {{ item.fromUser.username }} 希望注册为会员，理由是：{{ item.content }}
                    <template #actions v-if="authState.role === 'ADMIN'">
                      <div v-if="!item.read" class="optional-buttons">
                        <div
                          class="mark-approve-button-item"
                          @click="approve(item.fromUser.id, item.id)"
                        >
                          同意
                        </div>
                        <div
                          class="mark-reject-button-item"
                          @click="reject(item.fromUser.id, item.id)"
                        >
                          拒绝
                        </div>
                      </div>
                      <div v-else class="has_read_button" @click="markRead(item.id)">已读</div>
                    </template>
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_REVIEWED' && item.approved">
                  <NotificationContainer :item="item" :markRead="markRead">
                    您发布的帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    已审核通过
                  </NotificationContainer>
                </template>
                <template v-else-if="item.type === 'POST_REVIEWED' && item.approved === false">
                  <NotificationContainer :item="item" :markRead="markRead">
                    您发布的帖子
                    <router-link
                      class="notif-content-text"
                      @click="markRead(item.id)"
                      :to="`/posts/${item.post.id}`"
                    >
                      {{ stripMarkdownLength(item.post.title, 100) }}
                    </router-link>
                    已被管理员拒绝
                  </NotificationContainer>
                </template>
                <template v-else>
                  <NotificationContainer :item="item" :markRead="markRead">
                    {{ formatType(item.type) }}
                  </NotificationContainer>
                </template>
              </span>
              <span class="notif-time">{{ TimeManager.format(item.createdAt) }}</span>
            </div>
          </template>
        </BaseTimeline>
      </div>
    </template>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { API_BASE_URL } from '~/main'
import BaseTimeline from '~/components/BaseTimeline.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import NotificationContainer from '~/components/NotificationContainer.vue'
import { getToken, authState } from '~/utils/auth'
import { markNotificationsRead, fetchUnreadCount, notificationState } from '~/utils/notification'
import { toast } from '~/main'
import { stripMarkdownLength } from '~/utils/markdown'
import TimeManager from '~/utils/time'
import { reactionEmojiMap } from '~/utils/reactions'

export default {
  name: 'MessagePageView',
  components: { BaseTimeline, BasePlaceholder, NotificationContainer },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const notifications = ref([])
    const isLoadingMessage = ref(false)
    const selectedTab = ref(
      ['all', 'unread', 'control'].includes(route.query.tab) ? route.query.tab : 'unread',
    )
    const notificationPrefs = ref([])
    const filteredNotifications = computed(() =>
      selectedTab.value === 'all'
        ? notifications.value
        : notifications.value.filter((n) => !n.read),
    )

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
      MENTION: 'fas fa-at',
    }

    const fetchNotifications = async () => {
      try {
        const token = getToken()
        if (!token) {
          toast.error('请先登录')
          return
        }
        isLoadingMessage.value = true
        notifications.value = []
        const res = await fetch(`${API_BASE_URL}/api/notifications`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
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
                router.push(`/users/${n.comment.author.id}`)
              },
            })
          } else if (n.type === 'REACTION') {
            notifications.value.push({
              ...n,
              emoji: reactionEmojiMap[n.reactionType],
              iconClick: () => {
                if (n.fromUser) {
                  markRead(n.id)
                  router.push(`/users/${n.fromUser.id}`)
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
                  router.push(`/users/${n.fromUser.id}`)
                }
              },
            })
          } else if (n.type === 'POST_UPDATED') {
            notifications.value.push({
              ...n,
              src: n.comment.author.avatar,
              iconClick: () => {
                markRead(n.id)
                router.push(`/users/${n.comment.author.id}`)
              },
            })
          } else if (n.type === 'USER_ACTIVITY') {
            notifications.value.push({
              ...n,
              src: n.comment.author.avatar,
              iconClick: () => {
                markRead(n.id)
                router.push(`/users/${n.comment.author.id}`)
              },
            })
          } else if (n.type === 'MENTION') {
            notifications.value.push({
              ...n,
              icon: iconMap[n.type],
              iconClick: () => {
                if (n.fromUser) {
                  markRead(n.id)
                  router.push(`/users/${n.fromUser.id}`)
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
                  router.push(`/users/${n.fromUser.id}`)
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
                  router.push(`/posts/${n.post.id}`)
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
                  router.push(`/posts/${n.post.id}`)
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
                  router.push(`/posts/${n.post.id}`)
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
      } catch (e) {
        console.error(e)
      }
    }

    const fetchPrefs = async () => {
      notificationPrefs.value = await fetchNotificationPreferences()
    }

    const togglePref = async (pref) => {
      const ok = await updateNotificationPreference(pref.type, !pref.enabled)
      if (ok) {
        pref.enabled = !pref.enabled
        await fetchNotifications()
        await fetchUnreadCount()
      } else {
        toast.error('操作失败')
      }
    }

    const approve = async (id, nid) => {
      const token = getToken()
      if (!token) return
      const res = await fetch(`${API_BASE_URL}/api/admin/users/${id}/approve`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        markRead(nid)
        toast.success('已同意')
      } else {
        toast.error('操作失败')
      }
    }

    const reject = async (id, nid) => {
      const token = getToken()
      if (!token) return
      const res = await fetch(`${API_BASE_URL}/api/admin/users/${id}/reject`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        markRead(nid)
        toast.success('已拒绝')
      } else {
        toast.error('操作失败')
      }
    }

    const formatType = (t) => {
      switch (t) {
        case 'POST_VIEWED':
          return '帖子被查看'
        case 'COMMENT_REPLY':
          return '有人回复了你'
        case 'REACTION':
          return '有人点赞'
        case 'POST_REVIEW_REQUEST':
          return '帖子待审核'
        case 'POST_REVIEWED':
          return '帖子审核结果'
        case 'POST_UPDATED':
          return '关注的帖子有新评论'
        case 'FOLLOWED_POST':
          return '关注的用户发布了新文章'
        case 'POST_SUBSCRIBED':
          return '有人订阅了你的文章'
        case 'POST_UNSUBSCRIBED':
          return '有人取消订阅你的文章'
        case 'USER_FOLLOWED':
          return '有人关注了你'
        case 'USER_UNFOLLOWED':
          return '有人取消关注你'
        case 'USER_ACTIVITY':
          return '关注的用户有新动态'
        case 'MENTION':
          return '有人提到了你'
        case 'REGISTER_REQUEST':
          return '有人申请注册'
        case 'ACTIVITY_REDEEM':
          return '有人申请兑换奶茶'
        default:
          return t
      }
    }

    onMounted(() => {
      fetchNotifications()
      fetchPrefs()
    })

    return {
      notifications,
      formatType,
      isLoadingMessage,
      stripMarkdownLength,
      markRead,
      approve,
      reject,
      TimeManager,
      selectedTab,
      filteredNotifications,
      markAllRead,
      authState,
      notificationPrefs,
      togglePref,
    }
  },
}
</script>

<style scoped>
.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.message-page {
  background-color: var(--background-color);
  overflow-x: hidden;
}

.message-page-header {
  position: sticky;
  top: 1px;
  z-index: 200;
  background-color: var(--background-color-blur);
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.message-page-header-right {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.message-page-header-right-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
  color: var(--primary-color);
  padding-right: 10px;
  gap: 5px;
}

.message-page-header-right-item-button-icon {
  font-size: 12px;
}

.message-page-header-right-item-button-text {
  font-size: 12px;
}

.message-page-header-right-item-button-text:hover {
  text-decoration: underline;
}

.timeline-container {
  padding: 10px 20px;
  height: 100%;
}

.notif-content {
  display: flex;
  flex-direction: column;
  margin-bottom: 30px;
  position: relative;
}

.notif-content.read {
  opacity: 0.7;
}

.unread-dot {
  position: absolute;
  left: -10px;
  top: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
}

.notif-type {
  font-weight: bold;
}

.notif-time {
  font-size: 12px;
  color: gray;
}

.notif-content-text {
  font-weight: bold;
  color: var(--primary-color) !important;
  text-decoration: none !important;
  word-break: break-all;
}

.optional-buttons {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.mark-approve-button-item {
  color: green;
  cursor: pointer;
}

.mark-reject-button-item {
  color: red;
  cursor: pointer;
}

.mark-approve-button-item:hover {
  text-decoration: underline;
}

.mark-reject-button-item:hover {
  text-decoration: underline;
}

.has_read_button {
  font-size: 12px;
}

.notif-content-text:hover {
  color: var(--primary-color) !important;
  text-decoration: underline !important;
}

.notif-user {
  font-weight: bold;
  color: var(--text-color);
}

.message-tabs {
  display: flex;
  flex-direction: row;
  border-bottom: 1px solid var(--normal-border-color);
}

.message-tab-item {
  padding: 10px 20px;
  cursor: pointer;
}

.message-tab-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.message-control-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
}

.message-control-container {
  padding: 20px;
}

.message-control-push-item-container {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
}

.message-control-push-item {
  font-size: 14px;
  margin-bottom: 5px;
  padding: 8px 16px;
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.message-control-push-item.select {
  background-color: var(--primary-color);
  color: white;
}

@media (max-width: 768px) {
  .has_read_button {
    display: none;
  }
}
</style>
