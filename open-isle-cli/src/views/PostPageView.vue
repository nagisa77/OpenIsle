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
          <div class="article-pending-button">
            PENDING
          </div>
          <div class="article-block-button">
            BLOCK
          </div>
          <div class="article-subscribe-button">
            <i class="fas fa-user-plus"></i>
            订阅文章
          </div>
          <div class="article-unsubscribe-button">
            <i class="fas fa-user-minus"></i>
            取消订阅
          </div>
          <Dropdown v-model="postAction" :fetch-options="fetchPostActions" :show-search="false" menu-min-width="auto" menu-class="post-action-menu">
            <template #display="{ toggle }">
              <div class="post-action-icon" @click="toggle">
                <i class="fas fa-ellipsis-v"></i>
              </div>
            </template>
            <template #option="{ option }">
              <div class="post-action-item">{{ option.name }}</div>
            </template>
          </Dropdown>
        </div>
      </div>

      <div class="info-content-container author-info-container">
        <div class="user-avatar-container" @click="gotoProfile">
          <div class="user-avatar-item">
            <img class="user-avatar-item-img" :src="author.avatar" alt="avatar">
          </div>
        </div>

        <div class="info-content">
          <div class="info-content-header">
            <div class="user-name">{{ author.username }}</div>
            <div class="post-time">{{ postTime }}</div>
          </div>
          <div class="info-content-text" v-html="renderMarkdown(postContent)"></div>

          <div class="article-footer-container">
            <ReactionsGroup v-model="postReactions" content-type="post" :content-id="postId">
              <div class="make-reaction-item copy-link" @click="copyPostLink">
                <i class="fas fa-link"></i>
              </div>
            </ReactionsGroup>
          </div>
        </div>
      </div>

      <CommentEditor @submit="postComment" :loading="isWaitingPostingComment" />

      <div class="comments-container">
        <BaseTimeline :items="comments">
          <template #item="{ item }">
            <CommentItem :key="item.id" :comment="item" :level="level + 1" :default-show-replies="item.openReplies" />
          </template>
        </BaseTimeline>
        <!-- <CommentItem
          v-for="comment in comments"
          :key="comment.id"
          :comment="comment"
          :level="0"
          :default-show-replies="comment.openReplies"
          ref="postItems"
        /> -->
      </div>
    </div>

    <div class="post-page-scroller-container">
      <div class="scroller">
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ postTime }}</div>
        <div class="scroller-middle">
          <input type="range" class="scroller-range" :max="totalPosts" :min="1" v-model.number="currentIndex"
            @input="onSliderInput" />
          <div class="scroller-index">{{ currentIndex }}/{{ totalPosts }}</div>
        </div>
        <div v-if="isWaitingFetchingPost" class="scroller-time">loading...</div>
        <div v-else class="scroller-time">{{ lastReplyTime }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import CommentItem from '../components/CommentItem.vue'
import CommentEditor from '../components/CommentEditor.vue'
import BaseTimeline from '../components/BaseTimeline.vue'
import ArticleTags from '../components/ArticleTags.vue'
import ArticleCategory from '../components/ArticleCategory.vue'
import ReactionsGroup from '../components/ReactionsGroup.vue'
import Dropdown from '../components/Dropdown.vue'
import { renderMarkdown } from '../utils/markdown'
import { API_BASE_URL, toast } from '../main'
import { getToken } from '../utils/auth'
import TimeManager from '../utils/time'
import { hatch } from 'ldrs'
import { useRouter } from 'vue-router'
hatch.register()

export default {
  name: 'PostPageView',
  components: { CommentItem, CommentEditor, BaseTimeline, ArticleTags, ArticleCategory, ReactionsGroup, Dropdown },
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
    const isWaitingFetchingPost = ref(false);
    const isWaitingPostingComment = ref(false);
    const postTime = ref('')
    const postItems = ref([])
    const mainContainer = ref(null)
    const currentIndex = ref(1)
    const postAction = ref(null)

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

    const mapComment = c => ({
      id: c.id,
      userName: c.author.username,
      time: TimeManager.format(c.createdAt),
      avatar: c.author.avatar,
      text: c.content,
      reactions: c.reactions || [],
      reply: (c.replies || []).map(mapComment),
      openReplies: false,
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

    const fetchPost = async () => {
      try {
        isWaitingFetchingPost.value = true;
        const res = await fetch(`${API_BASE_URL}/api/posts/${postId}`)
        isWaitingFetchingPost.value = false;
        if (!res.ok) return
        const data = await res.json()
        postContent.value = data.content
        author.value = data.author
        title.value = data.title
        category.value = data.category
        tags.value = data.tags || []
        postReactions.value = data.reactions || []
        comments.value = (data.comments || []).map(mapComment)
        postTime.value = TimeManager.format(data.createdAt)
        await nextTick()
        gatherPostItems()
      } catch (e) {
        console.error(e)
      }
    }

    const totalPosts = computed(() => comments.value.length + 1)
    const lastReplyTime = computed(() =>
      comments.value.length ? comments.value[comments.value.length - 1].time : postTime.value
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

    const onSliderInput = () => {
      const target = postItems.value[currentIndex.value - 1]
      if (target && mainContainer.value) {
        const top = getTopRelativeTo(target, mainContainer.value)
        mainContainer.value.scrollTo({ top, behavior: 'instant' })
      }
    }

    const fetchPostActions = () => {
      return [
        { id: 'approve', name: '通过审核' },
        { id: 'reject', name: '驳回' }
      ]
    }

    watch(postAction, val => {
      if (!val) return
      if (val === 'approve') toast.success('已通过')
      else if (val === 'reject') toast.error('已驳回')
      postAction.value = null
    })

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
          comments.value.push(mapComment(data))
          await nextTick()
          gatherPostItems()
          toast.success('评论成功')
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
      gatherPostItems()
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
      lastReplyTime,
      postItems,
      mainContainer,
      currentIndex,
      totalPosts,
      postReactions,
      postAction,
      fetchPostActions,
      postId,
      postComment,
      onSliderInput,
      onScroll: updateCurrentIndex,
      copyPostLink,
      renderMarkdown,
      isWaitingFetchingPost,
      isWaitingPostingComment,
      gotoProfile
    }
  }
}
</script>
<style>
.post-page-container {
  background-color: var(--background-color);
  display: flex;
  flex-direction: row;
  height: calc(100vh - var(--header-height));
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
  height: calc(100% - 40px);
  width: calc(85% - 40px);
}

.post-page-scroller-container {
  display: flex;
  flex-direction: column;
  width: 15%;
}

.scroller {
  margin-top: 20px;
  margin-left: 20px;
}

.scroller-time {
  font-size: 14px;
  opacity: 0.5;
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

.post-action-icon {
  cursor: pointer;
  padding: 5px;
}

.post-action-menu {
  right: 0;
  left: auto;
}

.post-action-item {
  padding: 8px 16px;
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
  border-bottom: 1px solid #e2e2e2;
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
  width: 100%;
  flex-direction: column;
  gap: 10px;
}

.info-content-header {
  display: flex;
  flex-direction: row;
  gap: 10px;
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
  opacity: 0.7;
  width: 100%;
}

.article-footer-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  margin-top: 60px;
}

.reactions-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
  width: 100%;
  justify-content: space-between;
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

.reactions-count {
  font-size: 16px;
  opacity: 0.5;
}

.make-reaction-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.make-reaction-item {
  cursor: pointer;
  padding: 10px;
  border-radius: 50%;
  opacity: 0.5;
  font-size: 20px;
}

.like-reaction {
  color: #ff0000;
}

.like-reaction:hover {
  background-color: #ffe2e2;
}

.copy-link:hover {
  background-color: #e2e2e2;
}
</style>
