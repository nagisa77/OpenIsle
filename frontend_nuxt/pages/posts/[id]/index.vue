<template>
  <div v-if="isRestricted" class="restricted-content">
    <template v-if="visibleScope === 'ONLY_ME'">
      <LoginOverlay
        text="这是一篇私密文章，仅作者本人及管理员可见"
        button-text="返回首页"
        button-link="/"
      />
    </template>

    <template v-else-if="visibleScope === 'ONLY_REGISTER'">
      <LoginOverlay
        text="这是一篇仅登录用户可见的文章，请先登录"
        button-text="登录"
        button-link="/login"
      />
    </template>
  </div>
  <div v-else class="post-page-container">
    <div v-if="isWaitingFetchingPost" class="loading-container">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <div v-else class="post-page-main-container" ref="mainContainer">
      <div class="article-title-container">
        <div class="article-title-container-left">
          <div class="article-title">{{ title }}</div>
          <div class="article-info-container">
            <ArticleCategory :category="category" />
            <ArticleTags :tags="tags" />
          </div>
        </div>
        <div class="article-title-container-right">
          <div v-if="status === 'PENDING'" class="article-pending-button">审核中</div>
          <div v-if="status === 'REJECTED'" class="article-block-button">已拒绝</div>
          <div v-if="!rssExcluded" class="article-featured-button">精品</div>
          <div v-if="closed" class="article-gray-button">已关闭</div>
          <div v-if="visibleScope === 'ONLY_ME'" class="article-gray-button">仅自己可见</div>
          <div v-if="visibleScope === 'ONLY_REGISTER'" class="article-gray-button">仅登录可见</div>
          <div
            v-if="!closed && loggedIn && !isAuthor && !subscribed"
            class="article-subscribe-button"
            @click="subscribePost"
          >
            <people-plus />
            <div class="article-subscribe-button-text">
              {{ isMobile ? '订阅' : '订阅文章' }}
            </div>
          </div>
          <div
            v-if="!closed && loggedIn && !isAuthor && subscribed"
            class="article-unsubscribe-button"
            @click="unsubscribePost"
          >
            <people-minus-one />
            <div class="article-unsubscribe-button-text">
              {{ isMobile ? '退订' : '取消订阅' }}
            </div>
          </div>
          <DropdownMenu v-if="articleMenuItems.length > 0" :items="articleMenuItems">
            <template #trigger>
              <more-one class="action-menu-icon" />
            </template>
          </DropdownMenu>
        </div>
      </div>

      <div class="info-content-container author-info-container">
        <div class="user-avatar-container" @click="gotoProfile">
          <div class="user-avatar-item">
            <BaseUserAvatar
              class="user-avatar-item-img"
              :src="author.avatar"
              :user-id="author.id"
              alt="avatar"
              :disable-link="true"
            />
          </div>
          <div v-if="isMobile" class="info-content-header">
            <div class="user-name">
              {{ author.username }}
              <medal-one class="medal-icon" />
              <NuxtLink
                v-if="author.displayMedal"
                class="user-medal"
                :to="`/users/${author.id}?tab=achievements`"
                >{{ getMedalTitle(author.displayMedal) }}</NuxtLink
              >
            </div>
            <div class="post-time">{{ postTime }}</div>
          </div>
        </div>

        <div class="info-content">
          <div v-if="!isMobile" class="info-content-header">
            <div class="user-name">
              {{ author.username }}
              <medal-one class="medal-icon" />
              <NuxtLink
                v-if="author.displayMedal"
                class="user-medal"
                :to="`/users/${author.id}?tab=achievements`"
                >{{ getMedalTitle(author.displayMedal) }}</NuxtLink
              >
            </div>
            <div class="post-time">{{ postTime }}</div>
          </div>
          <div
            class="info-content-text"
            v-html="renderMarkdown(postContent)"
            @click="handleContentClick"
          ></div>

          <div class="article-footer-container">
            <div class="article-option-container">
              <ReactionsGroup
                ref="postReactionsGroupRef"
                v-model="postReactions"
                content-type="post"
                :content-id="postId"
              />
              <DonateGroup :post-id="postId" :author-id="author.id" :is-author="isAuthor" />
            </div>
            <div class="article-footer-actions">
              <div
                class="reaction-action like-action"
                :class="{ selected: postLikedByMe }"
                @click="togglePostLike"
              >
                <like v-if="!postLikedByMe" />
                <like v-else theme="filled" />
                <span v-if="postLikeCount" class="reaction-count">{{ postLikeCount }}</span>
              </div>
              <div class="reaction-action copy-link" @click="copyPostLink">
                <link-icon />
              </div>
            </div>
          </div>
        </div>
      </div>

      <PostLottery v-if="lottery" :lottery="lottery" :post-id="postId" @refresh="refreshPost" />
      <ClientOnly>
        <PostPoll v-if="poll" :poll="poll" :post-id="postId" @refresh="refreshPost" />
      </ClientOnly>
      <div v-if="closed" class="post-close-container">该帖子已关闭，内容仅供阅读，无法进行互动</div>

      <ClientOnly>
        <CommentEditor
          @submit="postComment"
          :loading="isWaitingPostingComment"
          :disabled="!loggedIn || closed"
          :show-login-overlay="!loggedIn"
          :parent-user-name="author.username"
        />
      </ClientOnly>

      <div class="comment-config-container">
        <div class="comment-sort-container">
          <div class="comment-sort-title">Sort by:</div>
          <Dropdown v-model="commentSort" :fetch-options="fetchCommentSorts" />
        </div>
      </div>

      <div v-if="isFetchingComments" class="loading-container">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <div v-else class="comments-container">
        <BasePlaceholder v-if="timelineItems.length === 0" text="暂无评论" icon="inbox" />
        <BaseTimeline v-else :items="timelineItems">
          <template #item="{ item }">
            <CommentItem
              v-if="item.kind === 'comment'"
              :key="item.id"
              :comment="item"
              :level="0"
              :default-show-replies="item.openReplies"
              :post-author-id="author.id"
              :post-closed="closed"
              @deleted="onCommentDeleted"
            />
            <PostChangeLogItem v-else :log="item" :title="title" />
          </template>
        </BaseTimeline>
      </div>
    </div>

    <vue-easy-lightbox
      :visible="lightboxVisible"
      :index="lightboxIndex"
      :imgs="lightboxImgs"
      @hide="lightboxVisible = false"
    />
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  nextTick,
  watch,
  watchEffect,
  onActivated,
} from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { useRoute } from 'vue-router'
import CommentItem from '~/components/CommentItem.vue'
import CommentEditor from '~/components/CommentEditor.vue'
import BaseTimeline from '~/components/BaseTimeline.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import PostChangeLogItem from '~/components/PostChangeLogItem.vue'
import ArticleTags from '~/components/ArticleTags.vue'
import ArticleCategory from '~/components/ArticleCategory.vue'
import ReactionsGroup from '~/components/ReactionsGroup.vue'
import DonateGroup from '~/components/DonateGroup.vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import PostLottery from '~/components/PostLottery.vue'
import PostPoll from '~/components/PostPoll.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import { renderMarkdown, handleMarkdownClick, stripMarkdownLength } from '~/utils/markdown'
import { getMedalTitle } from '~/utils/medal'
import { toast } from '~/main'
import { getToken, authState } from '~/utils/auth'
import TimeManager from '~/utils/time'
import { useIsMobile } from '~/utils/screen'
import Dropdown from '~/components/Dropdown.vue'
import { ClientOnly } from '#components'
import { useConfirm } from '~/composables/useConfirm'
import { Lock } from '@icon-park/vue-next'
const { confirm } = useConfirm()

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const route = useRoute()
const postId = route.params.id

const title = ref('')
const author = ref('')
const postContent = ref('')
const category = ref('')
const tags = ref([])
const visibleScope = ref('ALL') // 可见范围
const isRestricted = computed(() => {
  return (
    (visibleScope.value === 'ONLY_ME' && !isAuthor.value && !isAdmin.value) ||
    (visibleScope.value === 'ONLY_REGISTER' && !loggedIn.value)
  )
})
const postReactions = ref([])
const postReactionsGroupRef = ref(null)
const postLikeCount = computed(
  () => postReactions.value.filter((reaction) => reaction.type === 'LIKE').length,
)
const postLikedByMe = computed(() =>
  postReactions.value.some(
    (reaction) => reaction.type === 'LIKE' && reaction.user === authState.username,
  ),
)
const togglePostLike = () => {
  postReactionsGroupRef.value?.toggleReaction('LIKE')
}
const comments = ref([])
const changeLogs = ref([])
const status = ref('PUBLISHED')
const closed = ref(false)
const pinnedAt = ref(null)
const rssExcluded = ref(false)
const isWaitingPostingComment = ref(false)
const postTime = ref('')
const postItems = ref([])
const mainContainer = ref(null)
const currentIndex = ref(1)
const subscribed = ref(false)
const commentSort = ref('NEWEST')
const isFetchingComments = ref(false)
const isMobile = useIsMobile()
const timelineItems = ref([])

const headerHeight = import.meta.client
  ? parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--header-height')) || 0
  : 0

useHead(() => ({
  title: title.value ? `OpenIsle - ${title.value}` : 'OpenIsle',
  meta: [
    {
      name: 'description',
      content: stripMarkdownLength(postContent.value, 400),
    },
  ],
}))

if (import.meta.client) {
  onBeforeUnmount(() => {
    window.removeEventListener('scroll', updateCurrentIndex)
  })
}

const lightboxVisible = ref(false)
const lightboxIndex = ref(0)
const lightboxImgs = ref([])
const loggedIn = computed(() => authState.loggedIn)
const isAdmin = computed(() => authState.role === 'ADMIN')
const isAuthor = computed(() => authState.username === author.value.username)
const lottery = ref(null)
const poll = ref(null)
const articleMenuItems = computed(() => {
  const items = []
  if (isAuthor.value || isAdmin.value) {
    items.push({ text: '编辑文章', onClick: () => editPost() })
    items.push({ text: '删除文章', color: 'red', onClick: deletePost })
    if (closed.value) {
      items.push({ text: '重新打开帖子', onClick: () => reopenPost() })
    } else {
      items.push({ text: '关闭帖子', onClick: () => closePost() })
    }
  }
  if (isAdmin.value) {
    if (pinnedAt.value) {
      items.push({ text: '取消置顶', onClick: () => unpinPost() })
    } else {
      items.push({ text: '置顶', onClick: () => pinPost() })
    }
    if (rssExcluded.value) {
      items.push({ text: 'rss推荐', onClick: () => includeRss() })
    } else {
      items.push({ text: '取消rss推荐', onClick: () => excludeRss() })
    }
  }
  if (isAdmin.value && status.value === 'PENDING') {
    items.push({ text: '通过审核', onClick: () => approvePost() })
    items.push({ text: '驳回', color: 'red', onClick: () => rejectPost() })
  }
  return items
})

const gatherPostItems = () => {
  const items = []
  if (mainContainer.value) {
    const main = mainContainer.value.querySelector('.info-content-container')
    if (main) items.push({ el: main, top: getTop(main) })

    for (const c of timelineItems.value) {
      let el
      if (c.kind === 'comment') {
        el = document.getElementById('comment-' + c.id)
      } else {
        el = document.getElementById('change-log-' + c.id)
      }
      if (el) {
        items.push({ el, top: getTop(el) })
      }
    }
    // 根据 top 排序，防止评论异步插入后顺序错乱
    items.sort((a, b) => a.top - b.top)
    postItems.value = items.map((i) => i.el)
  }
}

const mapComment = (
  c,
  parentUserName = '',
  parentUserAvatar = '',
  parentUserId = '',
  level = 0,
) => ({
  id: c.id,
  kind: 'comment',
  userName: c.author.username,
  medal: c.author.displayMedal,
  userId: c.author.id,
  time: TimeManager.format(c.createdAt),
  avatar: c.author.avatar,
  text: c.content,
  reactions: c.reactions || [],
  pinned: Boolean(c.pinned ?? c.pinnedAt ?? c.pinned_at),
  isBot: Boolean(c.author?.bot),
  reply: (c.replies || []).map((r) =>
    mapComment(r, c.author.username, c.author.avatar, c.author.id, level + 1),
  ),
  openReplies: level === 0,
  src: c.author.avatar,
  createdAt: c.createdAt,
  iconClick: () => navigateTo(`/users/${c.author.id}`),
  parentUserName: parentUserName,
  parentUserAvatar: parentUserAvatar,
  parentUserId: parentUserId,
})

const changeLogIcon = (l) => {
  if (l.type === 'CONTENT') {
    return 'edit'
  } else if (l.type === 'TITLE') {
    return 'hashtag-key'
  } else if (l.type === 'CATEGORY') {
    return 'tag-one'
  } else if (l.type === 'TAG') {
    return 'tag-one'
  } else if (l.type === 'CLOSED') {
    if (l.newClosed) {
      return 'lock-one'
    } else {
      return 'unlock'
    }
  } else if (l.type === 'PINNED') {
    if (l.newPinnedAt) {
      return 'pin'
    } else {
      return 'clear-icon'
    }
  } else if (l.type === 'FEATURED') {
    if (l.newFeatured) {
      return 'star'
    } else {
      return 'dislike'
    }
  } else if (l.type === 'VISIBLE_SCOPE') {
    if (l.newVisibleScope === 'ONLY_ME') {
      return 'lock-one'
    } else if (l.newVisibleScope === 'ONLY_REGISTER') {
      return 'peoples-two'
    } else {
      return 'communication'
    }
  } else if (l.type === 'VOTE_RESULT') {
    return 'check-one'
  } else if (l.type === 'LOTTERY_RESULT') {
    return 'gift'
  } else if (l.type === 'DONATE') {
    return 'financing'
  } else {
    return 'info'
  }
}

const mapChangeLog = (l) => ({
  id: l.id,
  kind: 'log',
  username: l.username,
  userAvatar: l.userAvatar,
  type: l.type,
  createdAt: l.time,
  time: TimeManager.format(l.time),
  newClosed: l.newClosed,
  newPinnedAt: l.newPinnedAt,
  newFeatured: l.newFeatured,
  oldContent: l.oldContent,
  newContent: l.newContent,
  oldTitle: l.oldTitle,
  newTitle: l.newTitle,
  oldCategory: l.oldCategory,
  newCategory: l.newCategory,
  oldTags: l.oldTags,
  newTags: l.newTags,
  oldVisibleScope: l.oldVisibleScope,
  newVisibleScope: l.newVisibleScope,
  amount: l.amount,
  icon: changeLogIcon(l),
})

const getTop = (el) => {
  return el.getBoundingClientRect().top + window.scrollY
}

const findCommentPath = (id, list) => {
  for (const item of list) {
    if (item.id === Number(id) || item.id === id) {
      return [item]
    }
    if (item.reply && item.reply.length) {
      const sub = findCommentPath(id, item.reply)
      if (sub) return [item, ...sub]
    }
  }
  return null
}

const expandCommentPath = (id) => {
  const path = findCommentPath(id, comments.value)
  if (!path) return
  for (let i = 0; i < path.length - 1; i++) {
    path[i].openReplies = true
  }
}

const removeCommentFromList = (id, list) => {
  for (let i = 0; i < list.length; i++) {
    const item = list[i]
    if (item.id === id) {
      list.splice(i, 1)
      return true
    }
    if (item.reply && item.reply.length) {
      if (removeCommentFromList(id, item.reply)) return true
    }
  }
  return false
}

const handleContentClick = (e) => {
  handleMarkdownClick(e)
  if (e.target.tagName === 'IMG' && !e.target.classList.contains('emoji')) {
    const container = e.target.parentNode
    const imgs = [...container.querySelectorAll('img')].map((i) => i.src)
    lightboxImgs.value = imgs
    lightboxIndex.value = imgs.indexOf(e.target.src)
    lightboxVisible.value = true
  }
}

const onCommentDeleted = (id) => {
  removeCommentFromList(Number(id), comments.value)
  fetchTimeline()
}

const tokenHeader = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})
const {
  data: postData,
  pending: pendingPost,
  error: postError,
  refresh: refreshPost,
} = await useAsyncData(
  `post-${postId}`,
  async () => {
    try {
      return await $fetch(`${API_BASE_URL}/api/posts/${postId}`, { headers: tokenHeader.value })
    } catch (err) {}
  },
  {
    server: false,
    lazy: false,
  },
)

// 用 pendingPost 驱动现有 UI（替代 isWaitingFetchingPost 手控）
const isWaitingFetchingPost = computed(() => pendingPost.value)

// 同步到现有的响应式字段
watchEffect(() => {
  const data = postData.value
  if (!data) return
  postContent.value = data.content
  author.value = data.author
  title.value = data.title
  category.value = data.category
  tags.value = data.tags || []
  visibleScope.value = data.visibleScope || 'ALL'
  postReactions.value = data.reactions || []
  subscribed.value = !!data.subscribed
  status.value = data.status
  closed.value = data.closed
  pinnedAt.value = data.pinnedAt
  rssExcluded.value = data.rssExcluded
  postTime.value = TimeManager.format(data.createdAt)
  lottery.value = data.lottery || null
  poll.value = data.poll || null
})

// 404 客户端跳转
// if (postError.value?.statusCode === 404 && import.meta.client) {
//   router.replace('/404')
// }

const totalPosts = computed(() => timelineItems.value.length + 1)
const lastReplyTime = computed(() =>
  timelineItems.value.length
    ? timelineItems.value[timelineItems.value.length - 1].time
    : postTime.value,
)
const firstReplyTime = computed(() =>
  timelineItems.value.length ? timelineItems.value[0].time : postTime.value,
)
const scrollerTopTime = computed(() =>
  commentSort.value === 'OLDEST' ? postTime.value : firstReplyTime.value,
)

watch(
  () => timelineItems.value.length,
  async () => {
    await nextTick()
    gatherPostItems()
    updateCurrentIndex()
  },
)

const updateCurrentIndex = () => {
  const scrollTop = window.scrollY

  for (let i = 0; i < postItems.value.length; i++) {
    const el = postItems.value[i]
    const top = getTop(el)
    const bottom = top + el.offsetHeight

    if (bottom > scrollTop) {
      currentIndex.value = i + 1
      break
    }
  }
}

const onSliderInput = (e) => {
  const index = Number(e.target.value)
  currentIndex.value = index
  const target = postItems.value[index - 1]
  if (target) {
    const top = getTop(target) - headerHeight - 20 // 20 for beauty
    window.scrollTo({ top, behavior: 'auto' })
  }
}

const postComment = async (parentUserName, text, clear) => {
  if (!text.trim()) return
  if (closed.value) {
    toast.error('帖子已关闭')
    return
  }
  console.debug('Posting comment', { postId, text })
  isWaitingPostingComment.value = true
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    isWaitingPostingComment.value = false
    return
  }
  try {
    const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ content: text }),
    })
    console.debug('Post comment response status', res.status)
    if (res.ok) {
      const data = await res.json()
      console.debug('Post comment response data', data)
      await fetchTimeline()
      clear()
      if (data.reward && data.reward > 0) {
        toast.success(`评论成功，获得 ${data.reward} 经验值`)
      } else {
        toast.success('评论成功')
      }
    } else if (res.status === 429) {
      toast.error('评论过于频繁，请稍后再试')
    } else {
      toast.error(`评论失败: ${res.status} ${res.statusText}`)
    }
  } catch (e) {
    console.debug('Post comment error', e)
    toast.error(`评论失败: ${e.message}`)
  } finally {
    isWaitingPostingComment.value = false
  }
}

const copyPostLink = () => {
  navigator.clipboard.writeText(location.href.split('#')[0]).then(() => {
    toast.success('已复制')
  })
}

const subscribePost = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  const res = await fetch(`${API_BASE_URL}/api/subscriptions/posts/${postId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    subscribed.value = true
    toast.success('已订阅')
  } else {
    toast.error('操作失败')
  }
}

const approvePost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/approve`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    status.value = 'PUBLISHED'
    toast.success('已通过审核')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const pinPost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/pin`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    toast.success('已置顶')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const unpinPost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/unpin`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    toast.success('已取消置顶')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const excludeRss = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/rss-exclude`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    rssExcluded.value = true
    toast.success('已标记为rss不推荐')
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const includeRss = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/rss-include`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    rssExcluded.value = false
    toast.success('已标记为rss推荐')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const closePost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/close`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    closed.value = true
    toast.success('已关闭')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const reopenPost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/reopen`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    closed.value = false
    toast.success('已重新打开')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}

const editPost = () => {
  navigateTo(`/posts/${postId}/edit`, { replace: true })
}

const deletePost = async () => {
  try {
    const ok = await confirm('删除帖子', '此操作不可恢复，确认要删除吗？')
    if (!ok) return
    const token = getToken()
    if (!token) {
      toast.error('请先登录')
      return
    }
    const res = await fetch(`${API_BASE_URL}/api/posts/${postId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok) {
      toast.success('已删除')
      navigateTo('/', { replace: true })
    } else {
      toast.error('操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}

const rejectPost = async () => {
  const token = getToken()
  if (!token) return
  const res = await fetch(`${API_BASE_URL}/api/admin/posts/${postId}/reject`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    status.value = 'REJECTED'
    toast.success('已驳回')
    await refreshPost()
    await fetchTimeline()
  } else {
    toast.error('操作失败')
  }
}
const unsubscribePost = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }

  const res = await fetch(`${API_BASE_URL}/api/subscriptions/posts/${postId}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    subscribed.value = false
    toast.success('已取消订阅')
  } else {
    toast.error('操作失败')
  }
}

const fetchCommentSorts = () => {
  return Promise.resolve([
    { id: 'NEWEST', name: '最新', icon: 'lightning' },
    { id: 'OLDEST', name: '最旧', icon: 'history-icon' },
    // { id: 'MOST_INTERACTIONS', name: '最多互动', icon: 'fas fa-fire' }
  ])
}

const fetchCommentsAndChangeLog = async () => {
  isFetchingComments.value = true
  console.info('Fetching comments and chang log', { postId, sort: commentSort.value })
  try {
    const token = getToken()
    const res = await fetch(
      `${API_BASE_URL}/api/posts/${postId}/comments?sort=${commentSort.value}`,
      {
        headers: { Authorization: token ? `Bearer ${token}` : '' },
      },
    )
    console.info('Fetch comments response status', res.status)
    if (res.ok) {
      const data = await res.json()
      console.info('Fetched comments data', data)

      const commentList = []
      const changeLogList = []
      // 时间线列表，包含评论和日志
      const newTimelineItemList = []

      for (const item of data) {
        const mappedPayload =
          item.kind === 'comment' ? mapComment(item.payload) : mapChangeLog(item.payload)
        newTimelineItemList.push(mappedPayload)

        if (item.kind === 'comment') {
          commentList.push(mappedPayload)
        } else {
          changeLogList.push(mappedPayload)
        }
      }

      comments.value = commentList
      changeLogs.value = changeLogList
      timelineItems.value = newTimelineItemList

      isFetchingComments.value = false
      await nextTick()
      gatherPostItems()
    }
  } catch (e) {
    console.debug('Fetch comments error', e)
  } finally {
    isFetchingComments.value = false
  }
}

const fetchTimeline = async () => {
  await fetchCommentsAndChangeLog()
}

watch(commentSort, async () => {
  await fetchTimeline()
})

const jumpToHashComment = async () => {
  const hash = location.hash
  if (hash.startsWith('#comment-')) {
    const id = hash.substring('#comment-'.length)
    await new Promise((resolve) => setTimeout(resolve, 500))
    const el = document.getElementById('comment-' + id)
    if (el) {
      const top = el.getBoundingClientRect().top + window.scrollY - headerHeight - 20 // 20 for beauty
      window.scrollTo({ top, behavior: 'smooth' })
      el.classList.add('comment-highlight')
      setTimeout(() => el.classList.remove('comment-highlight'), 4000)
    }
  }
}

const gotoProfile = () => {
  navigateTo(`/users/${author.value.id}`, { replace: true })
}

const initPage = async () => {
  scrollTo(0, 0)
  await fetchTimeline()
  const hash = location.hash
  const id = hash.startsWith('#comment-') ? hash.substring('#comment-'.length) : null
  if (id) expandCommentPath(id)
  updateCurrentIndex()
  window.addEventListener('scroll', updateCurrentIndex)
  jumpToHashComment()
}

onActivated(async () => {
  await initPage()
})

onMounted(async () => {
  await initPage()
})
</script>

<style>
.post-page-container {
  background-color: var(--background-color);
  display: block;
  flex-direction: row;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  width: 85%;
}

.post-page-main-container {
  position: relative;
  scrollbar-width: none;
  padding: 20px;
  width: calc(100% - 40px);
}

.info-content-text p code {
  padding: 2px 4px;
}

.post-page-scroller-container {
  display: flex;
  flex-direction: column;
  width: 15%;
  position: sticky;
  top: var(--header-height);
  align-self: flex-start;
}

.comment-config-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 10px;
}

.comment-sort-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.post-close-container {
  padding: 40px;
  margin-top: 15px;
  text-align: center;
  font-size: 12px;
  color: var(--text-color);
  background-color: var(--background-color);
  border: 1px dashed var(--normal-border-color);
  border-radius: 10px;
  opacity: 0.5;
}

.scroller {
  margin-top: 20px;
  margin-left: 20px;
}

.scroller-time {
  font-size: 14px;
  opacity: 0.5;
}

.skeleton {
  background-color: #eee;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  height: 20px;
  margin-top: 5px;
}

.skeleton::before {
  content: '';
  display: block;
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  right: 0;
  background: linear-gradient(90deg, #eee 0%, #f5f5f7 40%, #e0e0e0 100%);
  transform: translateX(-100%);
  animation: skeleton-shimmer 1.5s infinite linear;
  z-index: 1;
  border-radius: 8px;
}
@keyframes skeleton-shimmer {
  100% {
    transform: translateX(100%);
  }
}

.user-avatar-container {
  display: flex;
  flex-direction: row;
}

.scroller-middle {
  margin: 10px 0;
  margin-left: 10px;
  display: flex;
  flex-direction: row;
  gap: 8px;
}

.medal-icon {
  font-size: 12px;
  margin-left: 4px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
}

.scroller-range {
  writing-mode: vertical-rl;
  direction: ltr;
  height: 300px;
  width: 2px;
  appearance: none;
  -webkit-appearance: none;
  background: transparent;
}

.scroller-range::-webkit-slider-runnable-track {
  width: 1px;
  height: 100%;
  background-color: var(--scroller-background-color);
}

.scroller-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 6px;
  height: 60px;
  right: 2px;
  border-radius: 3px;
  background-color: var(--scroller-background-color);
  cursor: pointer;
}

.author-info-container {
  margin-top: 20px;
}

.scroller-range::-moz-range-track {
  width: 2px;
  height: 100%;
  background-color: #ccc;
  border-radius: 1px;
}

.scroller-range::-moz-range-thumb {
  width: 10px;
  height: 10px;
  background-color: #333;
  border-radius: 50%;
  cursor: pointer;
}

.scroller-index {
  font-size: 17px;
  font-weight: bold;
  margin-top: 10px;
}

.article-title-container {
  display: flex;
  flex-direction: row;
  width: 100%;
  justify-content: space-between;
}

.article-title-container-left {
  display: flex;
  flex-direction: column;
}

.article-title-container-right {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.article-subscribe-button {
  background-color: var(--primary-color);
  color: white;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.article-gray-button,
.article-subscribe-button-text,
.article-featured-button,
.article-unsubscribe-button-text {
  white-space: nowrap;
}

.article-subscribe-button:hover {
  background-color: var(--primary-color-hover);
}

.article-unsubscribe-button {
  background-color: var(--background-color);
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;

  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.article-pending-button {
  background-color: var(--background-color);
  color: orange;
  border: 1px solid orange;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
}

.article-block-button {
  background-color: var(--background-color);
  color: red;
  border: 1px solid red;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
}

.article-featured-button {
  background-color: var(--background-color);
  color: var(--featured-color);
  border: 1px solid var(--featured-color);
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
}

.article-gray-button {
  background-color: var(--background-color);
  color: gray;
  border: 1px solid gray;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
}

.article-title {
  font-size: 30px;
  font-weight: bold;
}

.article-arrow-button {
  background-color: green;
  color: white;
  border: 1px solid green;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.article-reject-button {
  background-color: red;
  color: white;
  border: 1px solid red;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.action-menu-icon {
  cursor: pointer;
  font-size: 18px;
  padding: 5px;
}

.article-info-container {
  display: flex;
  flex-direction: row;
  margin-top: 10px;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.info-content-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  padding: 0px;
  border-bottom: 1px solid var(--normal-border-color);
}

.user-avatar-container {
  cursor: pointer;
}

.user-avatar-item {
  width: 50px;
  height: 50px;
}

.user-avatar-item-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.user-avatar-item-img :deep(.base-user-avatar-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
}

.info-content-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.user-name {
  font-size: 16px;
  font-weight: bold;
  opacity: 0.7;
}

.user-medal {
  font-size: 12px;
  margin-left: 4px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
  color: var(--text-color);
}

.post-time {
  font-size: 12px;
  opacity: 0.5;
}

.info-content-text {
  font-size: 16px;
  line-height: 1.5;
}

.article-footer-container {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 10px;
  margin-top: 0px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.article-option-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
}

.article-footer-actions {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.reaction-action {
  cursor: pointer;
  padding: 4px 10px;
  opacity: 0.6;
  border-radius: 10px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 5px;
  transition:
    background-color 0.2s ease,
    opacity 0.2s ease;
}

.reaction-action:hover {
  opacity: 1;
  background-color: var(--normal-light-background-color);
}

.reaction-action.like-action {
  color: #ff0000;
}

.reaction-action.selected {
  opacity: 1;
  background-color: var(--normal-light-background-color);
}

.reaction-count {
  font-size: 16px;
  font-weight: bold;
}

.reaction-action.copy-link:hover {
  background-color: #e2e2e2;
}

.comment-editor-wrapper {
  position: relative;
}

/* ======== 权限锁定状态 ======== */
.is-blurred {
  filter: blur(10px);
  pointer-events: none;
  user-select: none;
  transition: filter 0.3s ease;
}

/* 遮罩层 */
.restricted-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(12px);
  background: rgba(0, 0, 0, 0.45);
  animation: fadeIn 0.3s ease forwards;
}

/* 中央提示框 */
.restricted-content {
  background: #ffff;
  color: var(--primary-color);
  text-align: center;
}

.restricted-icon {
  font-size: 60px;
  opacity: 0.8;
  margin-bottom: 15px;
}

.restricted-button {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 18px;
  background: var(--primary-color);
  color: white;
  border-radius: 8px;
  text-decoration: none;
  transition: background 0.2s ease;
}

.restricted-button:hover {
  background: var(--primary-color-hover);
}

.restricted-actions {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .post-page-main-container {
    width: calc(100% - 20px);
    padding: 10px;
  }

  .article-title {
    font-size: 22px;
  }

  .user-name {
    font-size: 14px;
  }

  .user-medal {
    font-size: 12px;
  }

  .info-content-text {
    line-height: 1.5;
  }

  .reactions-viewer-item {
    font-size: 14px;
  }

  .post-page-scroller-container {
    display: none;
  }

  .info-content-container {
    flex-direction: column;
  }

  .info-content-header {
    width: calc(100% - 50px - 10px);
    margin-left: 10px;
  }

  .article-footer-container {
    margin-top: 0;
    margin-bottom: 0px;
  }

  .loading-container {
    width: 100%;
  }
}
</style>
