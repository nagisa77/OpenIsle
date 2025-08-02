<template>
  <div class="info-content-container" :id="'comment-' + comment.id" :style="{
    ...(level > 0 ? { /*borderLeft: '1px solid #e0e0e0', */borderBottom: 'none' } : {})
  }">
    <!-- <div class="user-avatar-container">
      <div class="user-avatar-item">
        <img class="user-avatar-item-img" :src="comment.avatar" alt="avatar" />
      </div>
    </div>   -->
    <div class="info-content">
      <div class="common-info-content-header">
        <div class="info-content-header-left">
          <div class="user-name">{{ comment.userName }}</div>
          <div class="post-time">{{ comment.time }}</div>
        </div>
        <div class="info-content-header-right">
          <DropdownMenu v-if="commentMenuItems.length > 0" :items="commentMenuItems">
            <template #trigger>
              <i class="fas fa-ellipsis-vertical action-menu-icon"></i>
            </template>
          </DropdownMenu>
        </div>
      </div>
      <div class="info-content-text" v-html="renderMarkdown(comment.text)" @click="handleContentClick"></div>
      <div class="article-footer-container">
        <ReactionsGroup v-model="comment.reactions" content-type="comment" :content-id="comment.id">
          <div class="make-reaction-item comment-reaction" @click="toggleEditor">
            <i class="far fa-comment"></i>
          </div>
          <div class="make-reaction-item copy-link" @click="copyCommentLink">
            <i class="fas fa-link"></i>
          </div>
        </ReactionsGroup>
      </div>
      <div class="comment-editor-wrapper">
        <CommentEditor v-if="showEditor" @submit="submitReply" :loading="isWaitingForReply" :disabled="!loggedIn" :show-login-overlay="!loggedIn" />
      </div>
      <div v-if="replyCount" class="reply-toggle" @click="toggleReplies">
        <i v-if="showReplies" class="fas fa-chevron-up reply-toggle-icon"></i>
        <i v-else class="fas fa-chevron-down reply-toggle-icon"></i>
        {{ replyCount }}条回复
      </div>
      <div v-if="showReplies" class="reply-list">
        <BaseTimeline :items="comment.reply">
          <template #item="{ item }">
            <CommentItem :key="item.id" :comment="item" :level="level + 1" :default-show-replies="item.openReplies" />
          </template>
        </BaseTimeline>
        <!-- <CommentItem
          v-for="r in comment.reply"
          :key="r.id"
          :comment="r"
          :level="level + 1"
          :default-show-replies="r.openReplies"
        /> -->
      </div>
      <vue-easy-lightbox
        :visible="lightboxVisible"
        :imgs="lightboxImgs"
        :index="lightboxIndex"
        @hide="lightboxVisible = false"
      />
    </div>
  </div>
</template>

<script>
import { ref, watch, computed } from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { useRouter } from 'vue-router'
import CommentEditor from './CommentEditor.vue'
import { renderMarkdown, handleMarkdownClick } from '../utils/markdown'
import TimeManager from '../utils/time'
import BaseTimeline from './BaseTimeline.vue'
import { API_BASE_URL, toast } from '../main'
import { getToken, authState } from '../utils/auth'
import ReactionsGroup from './ReactionsGroup.vue'
import DropdownMenu from './DropdownMenu.vue'
import LoginOverlay from './LoginOverlay.vue'

const CommentItem = {
  name: 'CommentItem',
  emits: ['deleted'],
  props: {
    comment: {
      type: Object,
      required: true
    },
    level: {
      type: Number,
      default: 0
    },
    defaultShowReplies: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { emit }) {
    const router = useRouter()
    const showReplies = ref(props.level === 0 ? true : props.defaultShowReplies)
    watch(
      () => props.defaultShowReplies,
      (val) => {
        showReplies.value = props.level === 0 ? true : val
      }
    )
    const showEditor = ref(false)
    const isWaitingForReply = ref(false)
    const lightboxVisible = ref(false)
    const lightboxIndex = ref(0)
    const lightboxImgs = ref([])
    const loggedIn = computed(() => authState.loggedIn)
    const countReplies = (list) => list.reduce((sum, r) => sum + 1 + countReplies(r.reply || []), 0)
    const replyCount = computed(() => countReplies(props.comment.reply || []))
    const toggleReplies = () => {
      showReplies.value = !showReplies.value
    }
    const toggleEditor = () => {
      showEditor.value = !showEditor.value
    }
    const isAuthor = computed(() => authState.username === props.comment.userName)
    const isAdmin = computed(() => authState.role === 'ADMIN')
    const commentMenuItems = computed(() =>
      (isAuthor.value || isAdmin.value) ? [{ text: '删除评论', color: 'red', onClick: () => deleteComment() }] : []
    )
    const deleteComment = async () => {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      const res = await fetch(`${API_BASE_URL}/api/comments/${props.comment.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (res.ok) {
        toast.success('已删除')
        emit('deleted', props.comment.id)
      } else {
        toast.error('操作失败')
      }
    }
    const submitReply = async (text) => {
      if (!text.trim()) return
      isWaitingForReply.value = true
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        isWaitingForReply.value = false
        return
      }
      try {
        const res = await fetch(`${API_BASE_URL}/api/comments/${props.comment.id}/replies`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ content: text })
        })
        if (res.ok) {
          const data = await res.json()
          const replyList = props.comment.reply || (props.comment.reply = [])
          replyList.push({
            id: data.id,
            userName: data.author.username,
            time: TimeManager.format(data.createdAt),
            avatar: data.author.avatar,
            text: data.content,
            reactions: [],
            reply: (data.replies || []).map(r => ({
              id: r.id,
              userName: r.author.username,
              time: TimeManager.format(r.createdAt),
              avatar: r.author.avatar,
              text: r.content,
              reactions: r.reactions || [],
              reply: [],
              openReplies: false,
              src: r.author.avatar,
              iconClick: () => router.push(`/users/${r.author.id}`)
            })),
            openReplies: false,
            src: data.author.avatar,
            iconClick: () => router.push(`/users/${data.author.id}`)
          })
          showEditor.value = false
          toast.success('回复成功')
        } else {
          toast.error('回复失败')
        }
      } catch (e) {
        toast.error('回复失败')
      } finally {
        isWaitingForReply.value = false
      }
    }
    const copyCommentLink = () => {
      const link = `${location.origin}${location.pathname}#comment-${props.comment.id}`
      navigator.clipboard.writeText(link).then(() => {
        toast.success('已复制')
      })
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
    return { showReplies, toggleReplies, showEditor, toggleEditor, submitReply, copyCommentLink, renderMarkdown, isWaitingForReply, commentMenuItems, deleteComment, lightboxVisible, lightboxIndex, lightboxImgs, handleContentClick, loggedIn, replyCount }
  }
}

CommentItem.components = { CommentItem, CommentEditor, BaseTimeline, ReactionsGroup, DropdownMenu, VueEasyLightbox, LoginOverlay }

export default CommentItem
</script>

<style scoped>
.reply-toggle {
  cursor: pointer;
  color: var(--primary-color);
  user-select: none;
}

.reply-list {}

.comment-reaction {
  color: var(--primary-color);
}

.comment-reaction:hover {
  background-color: lightgray;
}

.comment-highlight {
  animation: highlight 2s;
}

.reply-toggle-icon {
  margin-right: 5px;
}

.common-info-content-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}


@keyframes highlight {
  from {
    background-color: yellow;
  }

  to {
    background-color: transparent;
  }
}
</style>
