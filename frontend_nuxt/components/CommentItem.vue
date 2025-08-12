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
        <img class="user-avatar-item-img" :src="comment.avatar" alt="avatar" />
      </div>
    </div>   -->
    <div class="info-content">
      <div class="common-info-content-header">
        <div class="info-content-header-left">
          <span class="user-name">{{ comment.userName }}</span>
          <i class="fas fa-medal medal-icon"></i>
          <router-link
            v-if="comment.medal"
            class="medal-name"
            :to="`/users/${comment.userId}?tab=achievements`"
            >{{ getMedalTitle(comment.medal) }}</router-link
          >
          <span v-if="level >= 2">
            <i class="fas fa-reply reply-icon"></i>
            <span class="user-name reply-user-name">{{ comment.parentUserName }}</span>
          </span>
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
      <div
        class="info-content-text"
        v-html="renderMarkdown(comment.text)"
        @click="handleContentClick"
      ></div>
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
      <div class="comment-editor-wrapper" ref="editorWrapper">
        <CommentEditor
          v-if="showEditor"
          @submit="submitReply"
          :loading="isWaitingForReply"
          :disabled="!loggedIn"
          :show-login-overlay="!loggedIn"
          :parent-user-name="comment.userName"
        />
      </div>
      <div v-if="replyCount && level < 2" class="reply-toggle" @click="toggleReplies">
        <i v-if="showReplies" class="fas fa-chevron-up reply-toggle-icon"></i>
        <i v-else class="fas fa-chevron-down reply-toggle-icon"></i>
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

<script>
import { computed, ref, watch } from 'vue'
import VueEasyLightbox from 'vue-easy-lightbox'
import { useRouter } from 'vue-router'
import { API_BASE_URL, toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import { handleMarkdownClick, renderMarkdown } from '~/utils/markdown'
import { getMedalTitle } from '~/utils/medal'
import TimeManager from '~/utils/time'
import BaseTimeline from '~/components/BaseTimeline.vue'
import CommentEditor from '~/components/CommentEditor.vue'
import DropdownMenu from '~/components/DropdownMenu.vue'
import LoginOverlay from '~/components/LoginOverlay.vue'
import ReactionsGroup from '~/components/ReactionsGroup.vue'

const CommentItem = {
  name: 'CommentItem',
  emits: ['deleted'],
  props: {
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
  },
  setup(props, { emit }) {
    const router = useRouter()
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
    const countReplies = (list) => list.reduce((sum, r) => sum + 1 + countReplies(r.reply || []), 0)
    const replyCount = computed(() => countReplies(props.comment.reply || []))
    const toggleReplies = () => {
      showReplies.value = !showReplies.value
    }
    const toggleEditor = () => {
      showEditor.value = !showEditor.value
      if (showEditor.value) {
        setTimeout(() => {
          editorWrapper.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
        }, 100)
      }
    }

    // 合并所有子回复为一个扁平数组
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
    const isAdmin = computed(() => authState.role === 'ADMIN')
    const commentMenuItems = computed(() =>
      isAuthor.value || isAdmin.value
        ? [{ text: '删除评论', color: 'red', onClick: () => deleteComment() }]
        : [],
    )
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
              iconClick: () => router.push(`/users/${r.author.id}`),
            })),
            openReplies: false,
            src: data.author.avatar,
            iconClick: () => router.push(`/users/${data.author.id}`),
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
    const copyCommentLink = () => {
      const link = `${location.origin}${location.pathname}#comment-${props.comment.id}`
      navigator.clipboard.writeText(link).then(() => {
        toast.success('已复制')
      })
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
    return {
      showReplies,
      toggleReplies,
      showEditor,
      toggleEditor,
      submitReply,
      copyCommentLink,
      renderMarkdown,
      isWaitingForReply,
      commentMenuItems,
      deleteComment,
      lightboxVisible,
      lightboxIndex,
      lightboxImgs,
      handleContentClick,
      loggedIn,
      replyCount,
      replyList,
      getMedalTitle,
      editorWrapper,
    }
  },
}

CommentItem.components = {
  CommentItem,
  CommentEditor,
  BaseTimeline,
  ReactionsGroup,
  DropdownMenu,
  VueEasyLightbox,
  LoginOverlay,
}

export default CommentItem
</script>

<style scoped>
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

.reply-icon {
  margin-right: 10px;
  margin-left: 10px;
  opacity: 0.5;
}

.reply-user-name {
  opacity: 0.3;
}

.medal-name {
  font-size: 12px;
  margin-left: 1px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
  color: var(--text-color);
}

.medal-icon {
  font-size: 12px;
  opacity: 0.6;
  cursor: pointer;
  text-decoration: none;
  margin-left: 10px;
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
