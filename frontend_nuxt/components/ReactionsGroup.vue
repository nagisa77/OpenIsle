<template>
  <div class="reactions-container">
    <div class="reactions-viewer">
      <div
        class="reactions-viewer-item-container"
        @click="openPanel"
        @mouseenter="cancelHide"
        @mouseleave="scheduleHide"
      >
        <template v-if="displayedReactions.length">
          <div v-for="r in displayedReactions" :key="r.type" class="reactions-viewer-item">
            {{ reactionEmojiMap[r.type] }}
          </div>
          <div class="reactions-count">{{ totalCount }}</div>
        </template>
        <div v-else class="reactions-viewer-item placeholder">
          <i class="far fa-smile"></i>
          <span class="reactions-viewer-item-placeholder-text">点击以表态</span>
        </div>
      </div>
    </div>
    <div class="make-reaction-container">
      <div class="make-reaction-item like-reaction" @click="toggleReaction('LIKE')">
        <i v-if="!userReacted('LIKE')" class="far fa-heart"></i>
        <i v-else class="fas fa-heart"></i>
        <span class="reactions-count" v-if="likeCount">{{ likeCount }}</span>
      </div>
      <slot></slot>
    </div>
    <div
      v-if="panelVisible"
      class="reactions-panel"
      @mouseenter="cancelHide"
      @mouseleave="scheduleHide"
    >
      <div
        v-for="t in panelTypes"
        :key="t"
        class="reaction-option"
        @click="toggleReaction(t)"
        :class="{ selected: userReacted(t) }"
      >
        {{ reactionEmojiMap[t] }}<span v-if="counts[t]">{{ counts[t] }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, ref, watch } from 'vue'
import { API_BASE_URL, toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import { reactionEmojiMap } from '~/utils/reactions'

let cachedTypes = null
const fetchTypes = async () => {
  if (cachedTypes) return cachedTypes
  try {
    const token = getToken()
    const res = await fetch(`${API_BASE_URL}/api/reaction-types`, {
      headers: { Authorization: token ? `Bearer ${token}` : '' },
    })
    if (res.ok) {
      cachedTypes = await res.json()
    } else {
      cachedTypes = []
    }
  } catch {
    cachedTypes = []
  }
  return cachedTypes
}

export default {
  name: 'ReactionsGroup',
  props: {
    modelValue: { type: Array, default: () => [] },
    contentType: { type: String, required: true },
    contentId: { type: [Number, String], required: true },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const reactions = ref(props.modelValue)
    watch(
      () => props.modelValue,
      (v) => (reactions.value = v),
    )

    const reactionTypes = ref([])
    onMounted(async () => {
      reactionTypes.value = await fetchTypes()
    })

    const counts = computed(() => {
      const c = {}
      for (const r of reactions.value) {
        c[r.type] = (c[r.type] || 0) + 1
      }
      return c
    })

    const totalCount = computed(() => Object.values(counts.value).reduce((a, b) => a + b, 0))
    const likeCount = computed(() => counts.value['LIKE'] || 0)

    const userReacted = (type) =>
      reactions.value.some((r) => r.type === type && r.user === authState.username)

    const displayedReactions = computed(() => {
      return Object.entries(counts.value)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([type]) => ({ type }))
    })

    const panelTypes = computed(() => reactionTypes.value.filter((t) => t !== 'LIKE'))

    const panelVisible = ref(false)
    let hideTimer = null
    const openPanel = () => {
      clearTimeout(hideTimer)
      panelVisible.value = true
    }
    const scheduleHide = () => {
      clearTimeout(hideTimer)
      hideTimer = setTimeout(() => {
        panelVisible.value = false
      }, 500)
    }
    const cancelHide = () => {
      clearTimeout(hideTimer)
    }

    const toggleReaction = async (type) => {
      const token = getToken()
      if (!token) {
        toast.error('请先登录')
        return
      }
      const url =
        props.contentType === 'post'
          ? `${API_BASE_URL}/api/posts/${props.contentId}/reactions`
          : `${API_BASE_URL}/api/comments/${props.contentId}/reactions`

      // optimistic update
      const existingIdx = reactions.value.findIndex(
        (r) => r.type === type && r.user === authState.username,
      )
      let tempReaction = null
      let removedReaction = null
      if (existingIdx > -1) {
        removedReaction = reactions.value.splice(existingIdx, 1)[0]
      } else {
        tempReaction = { type, user: authState.username }
        reactions.value.push(tempReaction)
      }
      emit('update:modelValue', reactions.value)

      try {
        const res = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ type }),
        })
        if (res.ok) {
          if (res.status === 204) {
            // removal already reflected
          } else {
            const data = await res.json()
            const idx = tempReaction ? reactions.value.indexOf(tempReaction) : -1
            if (idx > -1) {
              reactions.value.splice(idx, 1, data)
            } else if (removedReaction) {
              // server added back reaction even though we removed? restore data
              reactions.value.push(data)
            }
            if (data.reward && data.reward > 0) {
              toast.success(`获得 ${data.reward} 经验值`)
            }
          }
          emit('update:modelValue', reactions.value)
        } else {
          // revert optimistic update on failure
          if (tempReaction) {
            const idx = reactions.value.indexOf(tempReaction)
            if (idx > -1) reactions.value.splice(idx, 1)
          } else if (removedReaction) {
            reactions.value.push(removedReaction)
          }
          emit('update:modelValue', reactions.value)
          toast.error('操作失败')
        }
      } catch (e) {
        if (tempReaction) {
          const idx = reactions.value.indexOf(tempReaction)
          if (idx > -1) reactions.value.splice(idx, 1)
        } else if (removedReaction) {
          reactions.value.push(removedReaction)
        }
        emit('update:modelValue', reactions.value)
        toast.error('操作失败')
      }
    }

    return {
      reactionEmojiMap,
      counts,
      totalCount,
      likeCount,
      displayedReactions,
      panelTypes,
      panelVisible,
      openPanel,
      scheduleHide,
      cancelHide,
      toggleReaction,
      userReacted,
    }
  },
}
</script>

<style>
.reactions-container {
  position: relative;
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
  cursor: pointer;
  color: #a2a2a2;
}

.reactions-viewer-item {
  font-size: 16px;
}

.reactions-viewer-item.placeholder {
  opacity: 0.5;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.reactions-viewer-item-placeholder-text {
  font-size: 14px;
  padding-left: 5px;
}

.make-reaction-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.make-reaction-item {
  cursor: pointer;
  padding: 10px;
  opacity: 0.5;
  border-radius: 8px;
  font-size: 20px;
}

.like-reaction {
  color: #ff0000;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 5px;
}

.make-reaction-item:hover {
  background-color: #ffe2e2;
}

.reactions-count {
  font-size: 16px;
  font-weight: bold;
}

.reactions-panel {
  position: absolute;
  bottom: 40px;
  left: -20px;
  background-color: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 5px;
  padding: 5px;
  max-width: 240px;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  z-index: 10;
  gap: 2px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
}

.reaction-option {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 2px;
}

.reaction-option.selected {
  background-color: var(--menu-selected-background-color);
}

@media (max-width: 768px) {
  .make-reaction-item {
    font-size: 16px;
    padding: 3px 5px;
  }
}
</style>
