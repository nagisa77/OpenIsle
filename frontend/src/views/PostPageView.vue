<template>
  <div class="post-page-container">
    <div v-if="isWaitingFetchingPost" class="loading-container">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <div v-else class="post-page-main-container" ref="mainContainer" @scroll="onScroll">
      <div class="article-title-container">
        <div class="article-title-container-left">
          <div class="article-title">{{ title }}</div>
          <div class="article-info-container">
            <ArticleCategory :category="category" />
            <ArticleTags :tags="tags" />
          </div>
        </div>
        <div class="article-title-container-right">
          <div v-if="status === 'PENDING'" class="article-pending-button">
            审核中
          </div>
          <div v-if="status === 'REJECTED'" class="article-block-button">
            已拒绝
          </div>
          <div v-if="loggedIn && !isAuthor && !subscribed" class="article-subscribe-button" @click="subscribePost">
            <i class="fas fa-user-plus"></i>
            <div class="article-subscribe-button-text">订阅文章</div>
          </div>
          <div v-if="loggedIn && !isAuthor && subscribed" class="article-unsubscribe-button" @click="unsubscribePost">
            <i class="fas fa-user-minus"></i>
            <div class="article-unsubscribe-button-text">取消订阅</div>
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
            <img class="user-avatar-item-img" :src="author.avatar" alt="avatar">
          </div>
          <div v-if="isMobile" class="info-content-header">
            <div class="user-name">{{ author.username }}</div>
            <div class="post-time">{{ postTime }}</div>
          </div>
        </div>

        <div class="info-content">
          <div v-if="!isMobile" class="info-content-header">
            <div class="user-name">{{ author.username }}</div>
            <div class="post-time">{{ postTime }}</div>
          </div>
          <div class="info-content-text" v-html="renderMarkdown(postContent)" @click="handleContentClick"></div>

          <div class="article-footer-container">
            <ReactionsGroup v-model="postReactions" content-type="post" :content-id="postId">
              <div class="make-reaction-item copy-link" @click="copyPostLink">
                <i class="fas fa-link"></i>
              </div>
            </ReactionsGroup>
          </div>
        </div>
      </div>

      <CommentEditor @submit="postComment" :loading="isWaitingPostingComment" :disabled="!loggedIn"
        :show-login-overlay="!loggedIn" />

      <div class="comment-config-container">
        <div class="comment-sort-container">
          <div class="comment-sort-title">Sort by: </div>
          <Dropdown v-model="commentSort" :fetch-options="fetchCommentSorts" />
        </div>
      </div>

      <div v-if="isFetchingComments" class="loading-container">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <div v-else class="comments-container">
        <BaseTimeline :items="comments"> 
          <template #item="{ item }">
            <CommentItem :key="item.id" :comment="item" :level="0" :default-show-replies="item.openReplies"
              @deleted="onCommentDeleted" />
          </template>
        </BaseTimeline>
      </div>
    </div>

    <div class="post-page-scroller-container">
      <div class="scroller">
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ scrollerTopTime }}</div>
        <div class="scroller-middle">
          <input type="range" class="scroller-range" :max="totalPosts" :min="1" v-model.number="currentIndex"
            @input="onSliderInput" />
          <div class="scroller-index">{{ currentIndex }}/{{ totalPosts }}</div>
        </div>
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ lastReplyTime }}</div>
      </div>
    </div>
    <vue-easy-lightbox :visible="lightboxVisible" :index="lightboxIndex" :imgs="lightboxImgs"
      @hide="lightboxVisible = false" />
  </div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { useRoute } from 'vue-router'
import CommentItem from '../components/CommentItem.vue'
import CommentEditor from '../components/CommentEditor.vue'
import BaseTimeline from '../components/BaseTimeline.vue'
import ArticleTags from '../components/ArticleTags.vue'
import ArticleCategory from '../components/ArticleCategory.vue'
import ReactionsGroup from '../components/ReactionsGroup.vue'
import DropdownMenu from '../components/DropdownMenu.vue'
import { renderMarkdown, handleMarkdownClick, stripMarkdownLength } from '../utils/markdown'
import { API_BASE_URL, toast } from '../main'
import { getToken, authState } from '../utils/auth'
import TimeManager from '../utils/time'
import { hatch } from 'ldrs'
import { useRouter } from 'vue-router'
import { isMobile } from '../utils/screen'
import Dropdown from '../components/Dropdown.vue'
hatch.register()

export default {
  name: 'PostPageView',
  components: { CommentItem, CommentEditor, BaseTimeline, ArticleTags, ArticleCategory, ReactionsGroup, DropdownMenu, VueEasyLightbox, Dropdown },
  setup() {
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
    const isWaitingFetchingPost = ref(false);
    const isWaitingPostingComment = ref(false);
    const postTime = ref('')
    const postItems = ref([])
    const mainContainer = ref(null)
    const currentIndex = ref(1)
    const subscribed = ref(false)
    const commentSort = ref('NEWEST')
    const isFetchingComments = ref(false)

    // record default metadata from the main document
    const defaultTitle = document.title
    const metaDescriptionEl = document.querySelector('meta[name="description"]')
    const defaultDescription = metaDescriptionEl ? metaDescriptionEl.getAttribute('content') : ''

    watch(title, t => {
      document.title = `OpenIsle - ${t}`
    })

    watch(postContent, c => {
      if (metaDescriptionEl) {
        metaDescriptionEl.setAttribute('content', stripMarkdownLength(c, 400))
      }
    })

    onBeforeUnmount(() => {
      document.title = defaultTitle
      if (metaDescriptionEl) metaDescriptionEl.setAttribute('content', defaultDescription)
    })
      
    const lightboxVisible = ref(false)
    const lightboxIndex = ref(0)
    const lightboxImgs = ref([])
    const loggedIn = computed(() => authState.loggedIn)
    const isAdmin = computed(() => authState.role === 'ADMIN')
    const isAuthor = computed(() => authState.username === author.value.username)
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
        if (main) items.push({ el: main, top: 0 })

        for (const c of comments.value) {
          const el = document.getElementById('comment-' + c.id)
          if (el) {
            items.push({ el, top: getTopRelativeTo(el, mainContainer.value) })
          }
        }
        // 根据 top 排序，防止评论异步插入后顺序错乱
        items.sort((a, b) => a.top - b.top)
        postItems.value = items.map(i => i.el)
      }
    }

    const mapComment = (c, level = 0) => ({
      id: c.id,
      userName: c.author.username,
      time: TimeManager.format(c.createdAt),
      avatar: c.author.avatar,
      text: c.content,
      reactions: c.reactions || [],
      reply: (c.replies || []).map(r => mapComment(r, level + 1)),
      openReplies: level === 0,
      src: c.author.avatar,
      iconClick: () => router.push(`/users/${c.author.id}`)
    })

    const getTopRelativeTo = (el, container) => {
      const elRect = el.getBoundingClientRect()
      const parentRect = container.getBoundingClientRect()
      // 加上 scrollTop，得到相对于 container 内部顶部的距离
      return elRect.top - parentRect.top + container.scrollTop
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

    const handleContentClick = e => {
      handleMarkdownClick(e)
      if (e.target.tagName === 'IMG') {
        const container = e.target.parentNode
        const imgs = [...container.querySelectorAll('img')].map(i => i.src)
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
        isWaitingFetchingPost.value = true;
        const token = getToken()
        const res = await fetch(`${API_BASE_URL}/api/posts/${postId}`, {
          headers: { Authorization: token ? `Bearer ${token}` : '' }
        })
        isWaitingFetchingPost.value = false;
        if (!res.ok) {
          if (res.status === 404) {
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
        await fetchComments()
        subscribed.value = !!data.subscribed
        status.value = data.status
        pinnedAt.value = data.pinnedAt
        postTime.value = TimeManager.format(data.createdAt)
        await nextTick()
      } catch (e) {
        console.error(e)
      }
    }

    const totalPosts = computed(() => comments.value.length + 1)
    const lastReplyTime = computed(() =>
      comments.value.length ? comments.value[comments.value.length - 1].time : postTime.value
    )
    const firstReplyTime = computed(() =>
      comments.value.length ? comments.value[0].time : postTime.value
    )
    const scrollerTopTime = computed(() =>
      commentSort.value === 'OLDEST' ? postTime.value : firstReplyTime.value
    )

    watch(
      () => comments.value.length,
      async () => {
        await nextTick()
        gatherPostItems()
      }
    )

    const updateCurrentIndex = () => {
      const container = mainContainer.value
      if (!container) return

      const scrollTop = container.scrollTop

      for (let i = 0; i < postItems.value.length; i++) {
        const el = postItems.value[i]
        // 计算元素相对 container 顶部的 top
        const top = getTopRelativeTo(el, container)
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
      if (target && mainContainer.value) {
        const top = getTopRelativeTo(target, mainContainer.value)
        mainContainer.value.scrollTo({ top, behavior: 'instant' })
      }
    }

    const postComment = async (text) => {
      if (!text.trim()) return
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
          body: JSON.stringify({ content: text })
        })
        if (res.ok) {
          const data = await res.json()
          await fetchComments()
          if (data.reward && data.reward > 0) {
            toast.success(`评论成功，获得 ${data.reward} 经验值`)
          } else {
            toast.success('评论成功')
          }
        } else if (res.status === 429) {
          toast.error('评论过于频繁，请稍后再试')
        } else {
          toast.error('评论失败')
        }
      } catch (e) {
        toast.error('评论失败')
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        headers: { Authorization: `Bearer ${token}` }
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
        { id: 'NEWEST', name: '最新', icon: 'fas fa-clock' },
        { id: 'OLDEST', name: '最旧', icon: 'fas fa-hourglass-start' },
        // { id: 'MOST_INTERACTIONS', name: '最多互动', icon: 'fas fa-fire' }
      ])
    }

    const fetchComments = async () => {
      isFetchingComments.value = true
      try {
        const token = getToken()
        const res = await fetch(`${API_BASE_URL}/api/posts/${postId}/comments?sort=${commentSort.value}`, {
          headers: { Authorization: token ? `Bearer ${token}` : '' }
        })
        if (res.ok) {
          const data = await res.json()
          comments.value = data.map(mapComment)
          isFetchingComments.value = false
          await nextTick()
          gatherPostItems()
        }
      } catch {
        // ignore
      } finally {
        isFetchingComments.value = false
      }
    }

    watch(commentSort, fetchComments)

    const jumpToHashComment = async () => {
      const hash = location.hash
      if (hash.startsWith('#comment-')) {
        const id = hash.substring('#comment-'.length)
        await nextTick()
        const el = document.getElementById('comment-' + id)
        if (el && mainContainer.value) {
          mainContainer.value.scrollTo({ top: getTopRelativeTo(el, mainContainer.value), behavior: 'instant' })
          el.classList.add('comment-highlight')
          setTimeout(() => el.classList.remove('comment-highlight'), 2000)
        }
      }
    }

    const gotoProfile = () => {
      router.push(`/users/${author.value.id}`)
    }

    onMounted(async () => {
      const hash = location.hash
      const id = hash.startsWith('#comment-') ? hash.substring('#comment-'.length) : null
      await fetchPost()
      if (id) expandCommentPath(id)
      updateCurrentIndex()
      await jumpToHashComment()
    })

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
      onScroll: updateCurrentIndex,
      copyPostLink,
      subscribePost,
      unsubscribePost,
      renderMarkdown,
      isWaitingFetchingPost,
      isWaitingPostingComment,
      gotoProfile,
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
      isFetchingComments
    }
  }
}
</script>
<style>
.post-page-container {
  background-color: var(--background-color);
  display: flex;
  flex-direction: row;
  height: 100vh;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  width: 85%;
}

.post-page-main-container {
  overflow-y: auto;
  scrollbar-width: none;
  padding: 20px;
  height: calc(100% - 40px - var(--header-height));
  width: calc(85% - 40px);
  padding-top: calc(var(--header-height) + 20px);
}

.post-page-scroller-container {
  padding-top: var(--header-height);
  display: flex;
  flex-direction: column;
  width: 15%;
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

.post-time {
  font-size: 14px;
  opacity: 0.5;
}

.info-content-text {
  font-size: 16px;
  line-height: 1.8;
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

@media (max-width: 768px) {
  .post-page-main-container {
    width: calc(100% - 20px);
    padding: 10px;
    padding-top: calc(var(--header-height) + 10px);
  }

  .article-title {
    font-size: 22px;
  }

  .user-name {
    font-size: 14px;
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
}
</style>
