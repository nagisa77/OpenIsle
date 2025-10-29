<template>
  <div
    class="info-content-container"
    :id="'comment-' + comment.id"
    :style="{
      ...(level > 0 ? { /*borderLeft: '1px solid #e0e0e0', */ borderBottom: 'none' } : {}),
    }"
  >
    <!-- <div class="user-avatar-container">
      <div class="user-avatar-item">
        <BaseImage class="user-avatar-item-img" :src="comment.avatar" alt="avatar" />
      </div>
    </div>   -->
    <div class="info-content">
      <div class="common-info-content-header">
        <div class="info-content-header-left">
          <span class="user-name">{{ comment.userName }}</span>
          <span v-if="isCommentFromPostAuthor" class="op-badge" title="楼主">OP</span>
          <span v-if="comment.isBot" class="bot-badge" title="Bot">Bot</span>
          <medal-one class="medal-icon" />
          <NuxtLink
            v-if="comment.medal"
            class="medal-name"
            :to="`/users/${comment.userId}?tab=achievements`"
            >{{ getMedalTitle(comment.medal) }}</NuxtLink
          >
          <pin v-if="comment.pinned" class="pin-icon" />
          <span v-if="level >= 2" class="reply-item">
            <next class="reply-icon" />
            <span class="reply-info">
              <BaseUserAvatar
                class="reply-avatar"
                :src="comment.parentUserAvatar"
                :user-id="comment.parentUserId"
                :alt="comment.parentUserName"
                :disable-link="!comment.parentUserId"
              />
              <span class="reply-user-name">{{ comment.parentUserName }}</span>
            </span>
          </span>
          <div class="post-time">{{ comment.time }}</div>
        </div>
        <div class="info-content-header-right">
          <DropdownMenu v-if="commentMenuItems.length > 0" :items="commentMenuItems">
            <template #trigger>
              <more-one class="action-menu-icon" />
            </template>
          </DropdownMenu>
        </div>
      </div>
      <div
        class="info-content-text"
        v-html="renderMarkdown(comment.text)"
        @click="handleContentClick"
      ></div>
      <div class="article-footer-container">
        <ReactionsGroup
          ref="commentReactionsGroupRef"
          v-model="comment.reactions"
          content-type="comment"
          :content-id="comment.id"
        />
        <div class="comment-reaction-actions">
          <div
            class="reaction-action like-action"
            :class="{ selected: commentLikedByMe }"
            @click="toggleCommentLike"
          >
            <like v-if="!commentLikedByMe" />
            <like v-else theme="filled" />
            <span v-if="commentLikeCount" class="reaction-count">{{ commentLikeCount }}</span>
          </div>
          <div class="reaction-action comment-reaction" @click="toggleEditor">
            <comment-icon />
          </div>
          <div class="reaction-action copy-link" @click="copyCommentLink">
            <link-icon />
          </div>
        </div>
      </div>
      <div class="comment-editor-wrapper" ref="editorWrapper">
        <CommentEditor
          v-if="showEditor"
          @submit="submitReply"
          :loading="isWaitingForReply"
          :disabled="!loggedIn || postClosed"
          :show-login-overlay="!loggedIn"
          :parent-user-name="comment.userName"
        />
      </div>
      <div v-if="replyCount && level < 2" class="reply-toggle" @click="toggleReplies">
        <up v-if="showReplies" class="reply-toggle-icon" />
        <down v-else class="reply-toggle-icon" />
        {{ replyCount }}条回复
      </div>
      <div v-if="showReplies && level < 2" class="reply-list">
        <BaseTimeline :items="replyList">
          <template #item="{ item }">
            <CommentItem
              :key="item.id"
              :comment="item"
              :level="level + 1"
              :default-show-replies="item.openReplies"
              :post-author-id="postAuthorId"
              :post-closed="postClosed"
            />
          </template>
        </BaseTimeline>
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

<script setup>
import { computed, ref, watch } from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import { handleMarkdownClick, renderMarkdown } from '~/utils/markdown'
import { getMedalTitle } from '~/utils/medal'
import TimeManager from '~/utils/time'
import BaseTimeline from '~/components/BaseTimeline.vue'
import CommentEditor from '~/components/CommentEditor.vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import ReactionsGroup from '~/components/ReactionsGroup.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const props = defineProps({
  comment: {
    type: Object,
    required: true,
  },
  level: {
    type: Number,
    default: 0,
  },
  defaultShowReplies: {
    type: Boolean,
    default: false,
  },
  postAuthorId: {
    type: [Number, String],
    required: true,
  },
  postClosed: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['deleted'])

const showReplies = ref(props.level === 0 ? true : props.defaultShowReplies)
watch(
  () => props.defaultShowReplies,
  (val) => {
    showReplies.value = props.level === 0 ? true : val
  },
)
const showEditor = ref(false)
const editorWrapper = ref(null)
const isWaitingForReply = ref(false)
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)
const lightboxImgs = ref([])
const loggedIn = computed(() => authState.loggedIn)
const commentReactionsGroupRef = ref(null)
const commentLikeCount = computed(
  () => (props.comment.reactions || []).filter((reaction) => reaction.type === 'LIKE').length,
)
const commentLikedByMe = computed(() =>
  (props.comment.reactions || []).some(
    (reaction) => reaction.type === 'LIKE' && reaction.user === authState.username,
  ),
)
const toggleCommentLike = () => {
  commentReactionsGroupRef.value?.toggleReaction('LIKE')
}
const countReplies = (list) => list.reduce((sum, r) => sum + 1 + countReplies(r.reply || []), 0)
const replyCount = computed(() => countReplies(props.comment.reply || []))
const isCommentFromPostAuthor = computed(() => {
  if (props.comment.userId == null || props.postAuthorId == null) {
    return false
  }
  return String(props.comment.userId) === String(props.postAuthorId)
})

const toggleReplies = () => {
  showReplies.value = !showReplies.value
}

const toggleEditor = () => {
  if (props.postClosed) return
  showEditor.value = !showEditor.value
  if (showEditor.value) {
    setTimeout(() => {
      editorWrapper.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }, 100)
  }
}

const flattenReplies = (list) => {
  let result = []
  for (const r of list) {
    result.push(r)
    if (r.reply && r.reply.length > 0) {
      result = result.concat(flattenReplies(r.reply))
    }
  }
  return result
}

const replyList = computed(() => {
  if (props.level < 1) {
    return props.comment.reply
  }

  return flattenReplies(props.comment.reply || [])
})

const isAuthor = computed(() => authState.username === props.comment.userName)
const isPostAuthor = computed(() => Number(authState.userId) === Number(props.postAuthorId))
const isAdmin = computed(() => authState.role === 'ADMIN')
const commentMenuItems = computed(() => {
  const items = []
  if (isAuthor.value || isAdmin.value) {
    items.push({ text: '删除评论', color: 'red', onClick: () => deleteComment() })
  }
  if (isAdmin.value || isPostAuthor.value) {
    if (props.comment.pinned) {
      items.push({ text: '取消置顶', onClick: () => unpinComment() })
    } else {
      items.push({ text: '置顶', onClick: () => pinComment() })
    }
  }
  return items
})
const deleteComment = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  console.debug('Deleting comment', props.comment.id)
  const res = await fetch(`${API_BASE_URL}/api/comments/${props.comment.id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  })
  console.debug('Delete comment response status', res.status)
  if (res.ok) {
    toast.success('已删除')
    emit('deleted', props.comment.id)
  } else {
    toast.error('操作失败')
  }
}
const submitReply = async (parentUserName, text, clear) => {
  if (!text.trim()) return
  if (props.postClosed) {
    toast.error('帖子已关闭')
    return
  }
  isWaitingForReply.value = true
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    isWaitingForReply.value = false
    return
  }
  console.debug('Submitting reply', { parentId: props.comment.id, text })
  try {
    const res = await fetch(`${API_BASE_URL}/api/comments/${props.comment.id}/replies`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ content: text }),
    })
    console.debug('Submit reply response status', res.status)
    if (res.ok) {
      const data = await res.json()
      console.debug('Submit reply response data', data)
      const replyList = props.comment.reply || (props.comment.reply = [])
      replyList.push({
        id: data.id,
        userName: data.author.username,
        time: TimeManager.format(data.createdAt),
        avatar: data.author.avatar,
        medal: data.author.displayMedal,
        text: data.content,
        parentUserName: parentUserName,
        parentUserAvatar: props.comment.avatar,
        parentUserId: props.comment.userId,
        reactions: [],
        reply: (data.replies || []).map((r) => ({
          id: r.id,
          userName: r.author.username,
          time: TimeManager.format(r.createdAt),
          avatar: r.author.avatar,
          text: r.content,
          reactions: r.reactions || [],
          reply: [],
          openReplies: false,
          src: r.author.avatar,
          userId: r.author.id,
          iconClick: () => navigateTo(`/users/${r.author.id}`),
        })),
        openReplies: false,
        src: data.author.avatar,
        userId: data.author.id,
        iconClick: () => navigateTo(`/users/${data.author.id}`),
      })
      clear()
      showEditor.value = false
      toast.success('回复成功')
    } else if (res.status === 429) {
      toast.error('回复过于频繁，请稍后再试')
    } else {
      toast.error(`回复失败: ${res.status} ${res.statusText}`)
    }
  } catch (e) {
    console.debug('Submit reply error', e)
    toast.error(`回复失败: ${e.message}`)
  } finally {
    isWaitingForReply.value = false
  }
}

const pinComment = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  const url = isAdmin.value
    ? `${API_BASE_URL}/api/admin/comments/${props.comment.id}/pin`
    : `${API_BASE_URL}/api/comments/${props.comment.id}/pin`
  const res = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    props.comment.pinned = true
    toast.success('已置顶')
  } else {
    toast.error('操作失败')
  }
}
const unpinComment = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  const url = isAdmin.value
    ? `${API_BASE_URL}/api/admin/comments/${props.comment.id}/unpin`
    : `${API_BASE_URL}/api/comments/${props.comment.id}/unpin`
  const res = await fetch(url, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    props.comment.pinned = false
    toast.success('已取消置顶')
  } else {
    toast.error('操作失败')
  }
}

const copyCommentLink = () => {
  const link = `${location.origin}${location.pathname}#comment-${props.comment.id}`
  navigator.clipboard.writeText(link).then(() => {
    toast.success('已复制')
  })
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
</script>

<style scoped>
.comment-reaction-actions {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.reaction-action {
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 10px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 5px;
  opacity: 0.6;
  font-size: 18px;
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
  font-size: 14px;
  font-weight: bold;
}

.reply-toggle {
  cursor: pointer;
  color: var(--primary-color);
  user-select: none;
}

.reply-list {
}

.comment-reaction {
  color: var(--primary-color);
}

.comment-highlight {
  animation: highlight 2s;
}

.reply-toggle-icon {
}

.common-info-content-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.reply-item,
.reply-info {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
}

.reply-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  margin-right: 5px;
  cursor: pointer;
}

.reply-icon {
  color: var(--primary-color);
  margin-left: 10px;
  margin-right: 10px;
  opacity: 0.5;
}

.reply-user-name {
  opacity: 0.3;
  display: none;
  font-weight: bold;
}

.article-footer-container {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  gap: 10px;
  margin-top: 0px;
  flex-wrap: wrap;
  margin-bottom: 0px;
}

.medal-name {
  font-size: 12px;
  margin-left: 1px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
  color: var(--text-color);
}

.op-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  padding: 0 6px;
  height: 18px;
  border-radius: 9px;
  background-color: rgba(242, 100, 25, 0.12);
  color: #f26419;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.bot-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  padding: 0 6px;
  height: 18px;
  border-radius: 9px;
  background-color: rgba(76, 175, 80, 0.16);
  color: #2e7d32;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.medal-icon {
  font-size: 12px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
  margin-left: 10px;
}

.pin-icon {
  font-size: 12px;
  margin-left: 10px;
  opacity: 0.6;
}

@keyframes highlight {
  from {
    background-color: yellow;
  }

  to {
    background-color: transparent;
  }
}

@media (max-width: 768px) {
  .reply-icon {
    margin-right: 3px;
    margin-left: 3px;
  }
}
</style>
