<template>
  <div class="post-page-container">
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
          <div
            v-if="loggedIn && !isAuthor && !subscribed"
            class="article-subscribe-button"
            @click="subscribePost"
          >
            <i class="fas fa-user-plus"></i>
            <div class="article-subscribe-button-text">
              {{ isMobile ? '订阅' : '订阅文章' }}
            </div>
          </div>
          <div
            v-if="loggedIn && !isAuthor && subscribed"
            class="article-unsubscribe-button"
            @click="unsubscribePost"
          >
            <i class="fas fa-user-minus"></i>
            <div class="article-unsubscribe-button-text">
              {{ isMobile ? '退订' : '取消订阅' }}
            </div>
          </div>
          <DropdownMenu v-if="articleMenuItems.length > 0" :items="articleMenuItems">
            <template #trigger>
              <i class="fas fa-ellipsis-vertical action-menu-icon"></i>
            </template>
          </DropdownMenu>
        </div>
      </div>

      <div class="info-content-container author-info-container">
        <div class="user-avatar-container" @click="gotoProfile">
          <div class="user-avatar-item">
            <img class="user-avatar-item-img" :src="author.avatar" alt="avatar" />
          </div>
          <div v-if="isMobile" class="info-content-header">
            <div class="user-name">
              {{ author.username }}
              <i class="fas fa-medal medal-icon"></i>
              <router-link
                v-if="author.displayMedal"
                class="user-medal"
                :to="`/users/${author.id}?tab=achievements`"
                >{{ getMedalTitle(author.displayMedal) }}</router-link
              >
            </div>
            <div class="post-time">{{ postTime }}</div>
          </div>
        </div>

        <div class="info-content">
          <div v-if="!isMobile" class="info-content-header">
            <div class="user-name">
              {{ author.username }}
              <i class="fas fa-medal medal-icon"></i>
              <router-link
                v-if="author.displayMedal"
                class="user-medal"
                :to="`/users/${author.id}?tab=achievements`"
                >{{ getMedalTitle(author.displayMedal) }}</router-link
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
            <ReactionsGroup v-model="postReactions" content-type="post" :content-id="postId">
              <div class="make-reaction-item copy-link" @click="copyPostLink">
                <i class="fas fa-link"></i>
              </div>
            </ReactionsGroup>
          </div>
        </div>
      </div>

      <div v-if="lottery" class="prize-container">
        <div class="prize-content">
          <div class="prize-info">
            <div class="prize-info-left">
              <div class="prize-icon">
                <img
                  class="prize-icon-img"
                  v-if="lottery.prizeIcon"
                  :src="lottery.prizeIcon"
                  alt="prize"
                />
                <i v-else class="fa-solid fa-gift default-prize-icon"></i>
              </div>
              <div class="prize-name">{{ lottery.prizeDescription }}</div>
              <div class="prize-count">x {{ lottery.prizeCount }}</div>
            </div>
            <div class="prize-end-time prize-info-right">
              <div v-if="!isMobile" class="prize-end-time-title">离结束还有</div>
              <div class="prize-end-time-value">{{ countdown }}</div>
              <div v-if="!isMobile" class="join-prize-button-container-desktop">
                <div
                  v-if="loggedIn && !hasJoined && !lotteryEnded"
                  class="join-prize-button"
                  @click="joinLottery"
                >
                  <div class="join-prize-button-text">参与抽奖</div>
                </div>
                <div v-else-if="hasJoined" class="join-prize-button-disabled">
                  <div class="join-prize-button-text">已参与</div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="isMobile" class="join-prize-button-container-mobile">
            <div
              v-if="loggedIn && !hasJoined && !lotteryEnded"
              class="join-prize-button"
              @click="joinLottery"
            >
              <div class="join-prize-button-text">参与抽奖</div>
            </div>
            <div v-else-if="hasJoined" class="join-prize-button-disabled">
              <div class="join-prize-button-text">已参与</div>
            </div>
          </div>
        </div>
        <div class="prize-member-container">
          <img
            v-for="p in lotteryParticipants"
            :key="p.id"
            class="prize-member-avatar"
            :src="p.avatar"
            alt="avatar"
            @click="gotoUser(p.id)"
          />
          <div v-if="lotteryEnded && lotteryWinners.length" class="prize-member-winner">
            <i class="fas fa-medal medal-icon"></i>
            <span class="prize-member-winner-name">获奖者: </span>
            <img
              v-for="w in lotteryWinners"
              :key="w.id"
              class="prize-member-avatar"
              :src="w.avatar"
              alt="avatar"
              @click="gotoUser(w.id)"
            />
            <div v-if="lotteryWinners.length === 1" class="prize-member-winner-name">
              {{ lotteryWinners[0].username }}
            </div>
          </div>
        </div>
      </div>

      <ClientOnly>
        <CommentEditor
          @submit="postComment"
          :loading="isWaitingPostingComment"
          :disabled="!loggedIn"
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
        <BaseTimeline :items="comments">
          <template #item="{ item }">
            <CommentItem
              :key="item.id"
              :comment="item"
              :level="0"
              :default-show-replies="item.openReplies"
              @deleted="onCommentDeleted"
            />
          </template>
        </BaseTimeline>
      </div>
    </div>

    <div class="post-page-scroller-container">
      <div class="scroller">
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ scrollerTopTime }}</div>
        <div class="scroller-middle">
          <input
            type="range"
            class="scroller-range"
            :max="totalPosts"
            :min="1"
            v-model.number="currentIndex"
            @input="onSliderInput"
          />
          <div class="scroller-index">{{ currentIndex }}/{{ totalPosts }}</div>
        </div>
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ lastReplyTime }}</div>
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

<script>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { useRoute } from 'vue-router'
import CommentItem from '~/components/CommentItem.vue'
import CommentEditor from '~/components/CommentEditor.vue'
import BaseTimeline from '~/components/BaseTimeline.vue'
import ArticleTags from '~/components/ArticleTags.vue'
import ArticleCategory from '~/components/ArticleCategory.vue'
import ReactionsGroup from '~/components/ReactionsGroup.vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import { renderMarkdown, handleMarkdownClick, stripMarkdownLength } from '~/utils/markdown'
import { getMedalTitle } from '~/utils/medal'
import { API_BASE_URL, toast } from '~/main'
import { getToken, authState } from '~/utils/auth'
import TimeManager from '~/utils/time'
import { useRouter } from 'vue-router'
import { useIsMobile } from '~/utils/screen'
import Dropdown from '~/components/Dropdown.vue'
import { ClientOnly } from '#components'

export default {
  name: 'PostPageView',
  components: {
    CommentItem,
    CommentEditor,
    BaseTimeline,
    ArticleTags,
    ArticleCategory,
    ReactionsGroup,
    DropdownMenu,
    VueEasyLightbox,
    Dropdown,
    ClientOnly,
  },
  async setup() {
    const route = useRoute()
    const postId = route.params.id
    const router = useRouter()

    const title = ref('')
    const author = ref('')
    const postContent = ref('')
    const category = ref('')
    const tags = ref([])
    const postReactions = ref([])
    const comments = ref([])
    const status = ref('PUBLISHED')
    const pinnedAt = ref(null)
    const isWaitingFetchingPost = ref(false)
    const isWaitingPostingComment = ref(false)
    const postTime = ref('')
    const postItems = ref([])
    const mainContainer = ref(null)
    const currentIndex = ref(1)
    const subscribed = ref(false)
    const commentSort = ref('NEWEST')
    const isFetchingComments = ref(false)
    const isMobile = useIsMobile()

    const headerHeight = process.client
      ? parseFloat(
          getComputedStyle(document.documentElement).getPropertyValue('--header-height'),
        ) || 0
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

    if (process.client) {
      onBeforeUnmount(() => {
        window.removeEventListener('scroll', updateCurrentIndex)
        if (countdownTimer) clearInterval(countdownTimer)
      })
    }

    const lightboxVisible = ref(false)
    const lightboxIndex = ref(0)
    const lightboxImgs = ref([])
    const loggedIn = computed(() => authState.loggedIn)
    const isAdmin = computed(() => authState.role === 'ADMIN')
    const isAuthor = computed(() => authState.username === author.value.username)
    const lottery = ref(null)
    const countdown = ref('00:00:00')
    let countdownTimer = null
    const lotteryParticipants = computed(() => lottery.value?.participants || [])
    const lotteryWinners = computed(() => lottery.value?.winners || [])
    const lotteryEnded = computed(() => {
      if (!lottery.value || !lottery.value.endTime) return false
      return new Date(lottery.value.endTime).getTime() <= Date.now()
    })
    const hasJoined = computed(() => {
      if (!loggedIn.value) return false
      return lotteryParticipants.value.some((p) => p.id === Number(authState.userId))
    })
    const updateCountdown = () => {
      if (!lottery.value || !lottery.value.endTime) {
        countdown.value = '00:00:00'
        return
      }
      const diff = new Date(lottery.value.endTime).getTime() - Date.now()
      if (diff <= 0) {
        countdown.value = '00:00:00'
        if (countdownTimer) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
        return
      }
      const h = String(Math.floor(diff / 3600000)).padStart(2, '0')
      const m = String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0')
      const s = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
      countdown.value = `${h}:${m}:${s}`
    }
    const startCountdown = () => {
      if (!process.client) return
      if (countdownTimer) clearInterval(countdownTimer)
      updateCountdown()
      countdownTimer = setInterval(updateCountdown, 1000)
    }
    const gotoUser = (id) => router.push(`/users/${id}`)
    const articleMenuItems = computed(() => {
      const items = []
      if (isAuthor.value || isAdmin.value) {
        items.push({ text: '编辑文章', onClick: () => editPost() })
        items.push({ text: '删除文章', color: 'red', onClick: () => deletePost() })
      }
      if (isAdmin.value) {
        if (pinnedAt.value) {
          items.push({ text: '取消置顶', onClick: () => unpinPost() })
        } else {
          items.push({ text: '置顶', onClick: () => pinPost() })
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

        for (const c of comments.value) {
          const el = document.getElementById('comment-' + c.id)
          if (el) {
            items.push({ el, top: getTop(el) })
          }
        }
        // 根据 top 排序，防止评论异步插入后顺序错乱
        items.sort((a, b) => a.top - b.top)
        postItems.value = items.map((i) => i.el)
      }
    }

    const mapComment = (c, parentUserName = '', level = 0) => ({
      id: c.id,
      userName: c.author.username,
      medal: c.author.displayMedal,
      userId: c.author.id,
      time: TimeManager.format(c.createdAt),
      avatar: c.author.avatar,
      text: c.content,
      reactions: c.reactions || [],
      reply: (c.replies || []).map((r) => mapComment(r, c.author.username, level + 1)),
      openReplies: level === 0,
      src: c.author.avatar,
      iconClick: () => router.push(`/users/${c.author.id}`),
      parentUserName: parentUserName,
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
      if (e.target.tagName === 'IMG') {
        const container = e.target.parentNode
        const imgs = [...container.querySelectorAll('img')].map((i) => i.src)
        lightboxImgs.value = imgs
        lightboxIndex.value = imgs.indexOf(e.target.src)
        lightboxVisible.value = true
      }
    }

    const onCommentDeleted = (id) => {
      removeCommentFromList(Number(id), comments.value)
      fetchComments()
    }

    const fetchPost = async () => {
      try {
        isWaitingFetchingPost.value = true
        const token = getToken()
        const res = await fetch(`${API_BASE_URL}/api/posts/${postId}`, {
          headers: { Authorization: token ? `Bearer ${token}` : '' },
        })
        isWaitingFetchingPost.value = false
        if (!res.ok) {
          if (res.status === 404 && process.client) {
            router.replace('/404')
          }
          return
        }
        const data = await res.json()
        postContent.value = data.content
        author.value = data.author
        title.value = data.title
        category.value = data.category
        tags.value = data.tags || []
        postReactions.value = data.reactions || []
        subscribed.value = !!data.subscribed
        status.value = data.status
        pinnedAt.value = data.pinnedAt
        postTime.value = TimeManager.format(data.createdAt)
        lottery.value = data.lottery || null
        if (lottery.value && lottery.value.endTime) startCountdown()
        await nextTick()
      } catch (e) {
        console.error(e)
      }
    }

    const totalPosts = computed(() => comments.value.length + 1)
    const lastReplyTime = computed(() =>
      comments.value.length ? comments.value[comments.value.length - 1].time : postTime.value,
    )
    const firstReplyTime = computed(() =>
      comments.value.length ? comments.value[0].time : postTime.value,
    )
    const scrollerTopTime = computed(() =>
      commentSort.value === 'OLDEST' ? postTime.value : firstReplyTime.value,
    )

    watch(
      () => comments.value.length,
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
          await fetchComments()
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
        pinnedAt.value = new Date().toISOString()
        toast.success('已置顶')
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
        pinnedAt.value = null
        toast.success('已取消置顶')
      } else {
        toast.error('操作失败')
      }
    }

    const editPost = () => {
      router.push(`/posts/${postId}/edit`)
    }

    const deletePost = async () => {
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
        router.push('/')
      } else {
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

    const joinLottery = async () => {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/lottery/join`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        toast.success('已参与抽奖')
        await fetchPost()
      } else {
        toast.error('操作失败')
      }
    }

    const fetchCommentSorts = () => {
      return Promise.resolve([
        { id: 'NEWEST', name: '最新', icon: 'fas fa-clock' },
        { id: 'OLDEST', name: '最旧', icon: 'fas fa-hourglass-start' },
        // { id: 'MOST_INTERACTIONS', name: '最多互动', icon: 'fas fa-fire' }
      ])
    }

    const fetchComments = async () => {
      isFetchingComments.value = true
      console.debug('Fetching comments', { postId, sort: commentSort.value })
      try {
        const token = getToken()
        const res = await fetch(
          `${API_BASE_URL}/api/posts/${postId}/comments?sort=${commentSort.value}`,
          {
            headers: { Authorization: token ? `Bearer ${token}` : '' },
          },
        )
        console.debug('Fetch comments response status', res.status)
        if (res.ok) {
          const data = await res.json()
          console.debug('Fetched comments count', data.length)
          comments.value = data.map(mapComment)
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

    watch(commentSort, fetchComments)

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
      router.push(`/users/${author.value.id}`)
    }

    onMounted(async () => {
      await fetchComments()
      const hash = location.hash
      const id = hash.startsWith('#comment-') ? hash.substring('#comment-'.length) : null
      if (id) expandCommentPath(id)
      updateCurrentIndex()
      window.addEventListener('scroll', updateCurrentIndex)
      jumpToHashComment()
    })

    await fetchPost()

    return {
      postContent,
      author,
      title,
      category,
      tags,
      comments,
      postTime,
      scrollerTopTime,
      lastReplyTime,
      postItems,
      mainContainer,
      currentIndex,
      totalPosts,
      postReactions,
      articleMenuItems,
      postId,
      postComment,
      onSliderInput,
      copyPostLink,
      subscribePost,
      unsubscribePost,
      joinLottery,
      renderMarkdown,
      isWaitingFetchingPost,
      isWaitingPostingComment,
      gotoProfile,
      gotoUser,
      subscribed,
      loggedIn,
      isAuthor,
      status,
      isAdmin,
      approvePost,
      editPost,
      onCommentDeleted,
      deletePost,
      pinPost,
      unpinPost,
      rejectPost,
      lightboxVisible,
      lightboxIndex,
      lightboxImgs,
      handleContentClick,
      isMobile,
      pinnedAt,
      commentSort,
      fetchCommentSorts,
      isFetchingComments,
      getMedalTitle,
      lottery,
      countdown,
      lotteryParticipants,
      lotteryWinners,
      lotteryEnded,
      hasJoined,
    }
  },
}
</script>
<style>
.post-page-container {
  background-color: var(--background-color);
  display: flex;
  flex-direction: row;
  height: 100%;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  width: 85%;
}

.post-page-main-container {
  scrollbar-width: none;
  padding: 20px;
  width: calc(85% - 40px);
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

.scroller {
  margin-top: 20px;
  margin-left: 20px;
}

.scroller-time {
  font-size: 14px;
  opacity: 0.5;
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

.article-subscribe-button-text,
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
  font-size: 14px;
  opacity: 0.5;
}

.info-content-text {
  font-size: 16px;
  line-height: 1.5;
  width: 100%;
}

.article-footer-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  margin-top: 0px;
}

.reactions-viewer {
  display: flex;
  flex-direction: row;
  gap: 20px;
  align-items: center;
}

.reactions-viewer-item-container {
  display: flex;
  flex-direction: row;
  gap: 2px;
  align-items: center;
}

.reactions-viewer-item {
  font-size: 16px;
}

.make-reaction-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.copy-link:hover {
  background-color: #e2e2e2;
}

.comment-editor-wrapper {
  position: relative;
}

.prize-container {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background-color: var(--lottery-background-color);
  border-radius: 10px;
  padding: 10px;
}

.prize-info {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  width: 100%;
  align-items: center;
}

.join-prize-button-container-mobile {
  margin-top: 15px;
  margin-bottom: 10px;
}

.prize-icon {
  width: 24px;
  height: 24px;
}

.default-prize-icon {
  font-size: 24px;
  opacity: 0.5;
}

.prize-icon-img {
  width: 100%;
  height: 100%;
}

.prize-name {
  font-size: 13px;
  opacity: 0.7;
  margin-left: 10px;
}

.prize-count {
  font-size: 13px;
  font-weight: bold;
  opacity: 0.7;
  margin-left: 10px;
  color: var(--primary-color);
}

.prize-end-time {
  display: flex;
  flex-direction: row;
  align-items: center;
  font-size: 13px;
  opacity: 0.7;
  margin-left: 10px;
}

.prize-end-time-title {
  font-size: 13px;
  opacity: 0.7;
  margin-right: 5px;
}

.prize-end-time-value {
  font-size: 13px;
  font-weight: bold;
  color: var(--primary-color);
}

.prize-info-left,
.prize-info-right {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.join-prize-button {
  margin-left: 10px;
  background-color: var(--primary-color);
  color: white;
  padding: 5px 10px;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
}

.join-prize-button:hover {
  background-color: var(--primary-color-hover);
}

.join-prize-button-disabled {
  text-align: center;
  margin-left: 10px;
  background-color: var(--primary-color);
  color: white;
  padding: 5px 10px;
  border-radius: 8px;
  background-color: var(--primary-color-disabled);
  opacity: 0.5;
  cursor: not-allowed;
}

.prize-member-avatar {
  width: 30px;
  height: 30px;
  margin-left: 3px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
}

.prize-member-winner {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
}

.medal-icon {
  font-size: 16px;
  color: var(--primary-color);
}

.prize-member-winner-name {
  font-size: 13px;
  opacity: 0.7;
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

  .post-time {
    font-size: 12px;
  }

  .info-content-text {
    line-height: 1.3;
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
  }

  .loading-container {
    width: 100%;
  }

  .join-prize-button,
  .join-prize-button-disabled {
    margin-left: 0;
  }
}
</style>
