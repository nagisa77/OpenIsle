<template>
  <div class="post-page-container">
    <div v-if="isWaitingFetchingPost" class="loading-container">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <div v-else class="post-page-main-container" ref="mainContainer" @scroll="onScroll">
      <div class="article-title-container">
        <div class="article-title">{{ title }}</div>
        <div class="article-info-container">
          <div class="article-info-item">
            <img class="article-info-item-img" :src="category.smallIcon" alt="category">
            <div class="article-info-item-text">{{ category.name }}</div>
          </div>

          <div class="article-tags-container">
            <div class="article-info-item" v-for="tag in tags" :key="tag">
              <img class="article-info-item-img" :src="tag.smallIcon" alt="tag">
              <div class="article-info-item-text">{{ tag.name }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="info-content-container" ref="postItems">
        <div class="user-avatar-container">
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
            <div class="reactions-container">
              <div class="reactions-viewer">
                <div class="reactions-viewer-item-container">
                  <div class="reactions-viewer-item">
                    🤣
                  </div>
                  <div class="reactions-viewer-item">
                    ❤️
                  </div>
                  <div class="reactions-viewer-item">
                    👏
                  </div>
                </div>
                <div class="reactions-count">1882</div>
              </div>

              <div class="make-reaction-container">
                <div class="make-reaction-item like-reaction">
                  <i class="far fa-heart"></i>
                </div>
                <div class="make-reaction-item copy-link" @click="copyPostLink">
                  <i class="fas fa-link"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <CommentEditor @submit="postComment" :loading="isWaitingPostingComment" />

      <div class="comments-container">
        <BaseTimeline :items="comments" >
          <template #item="{ item }">
            <CommentItem
              :key="item.id"
              :comment="item"
              :level="level + 1"
              :default-show-replies="item.openReplies"
              ref="postItems"
            />
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import CommentItem from '../components/CommentItem.vue'
import CommentEditor from '../components/CommentEditor.vue'
import BaseTimeline from '../components/BaseTimeline.vue'
import { renderMarkdown } from '../utils/markdown'
import { API_BASE_URL, toast } from '../main'
import { getToken } from '../utils/auth'
import { hatch } from 'ldrs'
hatch.register()

export default {
  name: 'PostPageView',
  components: { CommentItem, CommentEditor, BaseTimeline },
  setup() {
    const route = useRoute()
    const postId = route.params.id

    const title = ref('')
    const author = ref('')
    const postContent = ref('')
    const category = ref('')
    const tags = ref([])
    const comments = ref([])
    const isWaitingFetchingPost = ref(false);
    const isWaitingPostingComment = ref(false);
    const postTime = ref('')
    const postItems = ref([])
    const mainContainer = ref(null)
    const currentIndex = ref(1)

    const mapComment = c => ({
      id: c.id,
      userName: c.author.username,
      time: new Date(c.createdAt).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }),
      avatar: c.author.avatar,
      text: c.content,
      reply: (c.replies || []).map(mapComment),
      openReplies: false,
      src: c.author.avatar,
    })

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
        comments.value = (data.comments || []).map(mapComment)
        postTime.value = new Date(data.createdAt).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
      } catch (e) {
        console.error(e)
      }
    }

    const totalPosts = computed(() => comments.value.length + 1)
    const lastReplyTime = computed(() =>
      comments.value.length ? comments.value[comments.value.length - 1].time : postTime.value
    )

    const updateCurrentIndex = () => {
      const scrollTop = mainContainer.value ? mainContainer.value.scrollTop : 0
      for (let i = 0; i < postItems.value.length; i++) {
        const el = postItems.value[i].$el
        if (el.offsetTop + el.offsetHeight > scrollTop) {
          currentIndex.value = i + 1
          break
        }
      }
    }

    const onSliderInput = () => {
      const target = postItems.value[currentIndex.value - 1]?.$el
      if (target && mainContainer.value) {
        mainContainer.value.scrollTo({ top: target.offsetTop, behavior: 'instant' })
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
          comments.value.push(mapComment(data))
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
          mainContainer.value.scrollTo({ top: el.offsetTop, behavior: 'instant' })
          el.classList.add('comment-highlight')
          setTimeout(() => el.classList.remove('comment-highlight'), 2000)
        }
      }
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
      lastReplyTime,
      postItems,
      mainContainer,
      currentIndex,
      totalPosts,
      postComment,
      onSliderInput,
      onScroll: updateCurrentIndex,
      copyPostLink,
      renderMarkdown,
      isWaitingFetchingPost,
      isWaitingPostingComment
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
  flex-direction: column;
}

.article-title {
  font-size: 30px;
  font-weight: bold;
}

.article-info-container {
  display: flex;
  flex-direction: row;
  margin-top: 10px;
  gap: 10px;
  align-items: center;
}

.article-info-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
}

.article-tags-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.article-info-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  padding: 2px 4px;
  background-color: #f0f0f0;
}

.article-info-item-img {
  width: 16px;
  height: 16px;
}

.info-content-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  padding: 0px;
  border-bottom: 1px solid #e2e2e2;
}

.user-avatar-container {}

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
