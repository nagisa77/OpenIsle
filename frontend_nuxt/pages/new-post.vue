<template>
  <div class="new-post-page">
    <div class="new-post-form">
      <input class="post-title-input" v-model="title" placeholder="标题" />
      <div class="post-editor-container">
        <PostEditor v-model="content" v-model:loading="isAiLoading" :disabled="!isLogin" />
        <LoginOverlay v-if="!isLogin" />
      </div>
      <div class="post-options">
        <div class="post-options-left">
          <CategorySelect v-model="selectedCategory" />
          <TagSelect v-model="selectedTags" creatable />
          <PostTypeSelect v-model="postType" />
          <PostVisibleScopeSelect v-model="postVisibleScope"/>
        </div>
        <div class="post-options-right">
          <div class="post-clear" @click="clearPost"><clear-icon /> 清空</div>
          <div class="ai-generate" @click="aiGenerate">
            <smart-optimization />
            MD 格式优化
          </div>
          <div class="post-draft" @click="saveDraft">
            <save-icon />
            存草稿
          </div>
          <div
            v-if="!isWaitingPosting"
            class="post-submit"
            :class="{ disabled: !isLogin }"
            @click="submitPost"
          >
            发布
          </div>
          <div v-else class="post-submit-loading">
            <loading-four class="loading-icon" /> 发布中...
          </div>
        </div>
      </div>
      <LotteryForm v-if="postType === 'LOTTERY'" :data="lottery" />
      <PollForm v-if="postType === 'POLL'" :data="poll" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import CategorySelect from '~/components/CategorySelect.vue'
import LoginOverlay from '~/components/LoginOverlay.vue'
import PostEditor from '~/components/PostEditor.vue'
import PostTypeSelect from '~/components/PostTypeSelect.vue'
import TagSelect from '~/components/TagSelect.vue'
import LotteryForm from '~/components/LotteryForm.vue'
import PollForm from '~/components/PollForm.vue'
import { toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import PostVisibleScopeSelect from '~/components/PostVisibleScopeSelect.vue'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const title = ref('')
const content = ref('')
const selectedCategory = ref('')
const selectedTags = ref([])
const postType = ref('NORMAL')
const postVisibleScope = ref('ALL')
const lottery = reactive({
  prizeIcon: '',
  prizeIconFile: null,
  tempPrizeIcon: '',
  showPrizeCropper: false,
  prizeName: '',
  prizeDescription: '',
  prizeCount: 1,
  pointCost: 0,
  endTime: null,
})
const poll = reactive({
  options: ['', ''],
  endTime: null,
  multiple: false,
})
const startTime = ref(null)
const isWaitingPosting = ref(false)
const isAiLoading = ref(false)
const isLogin = computed(() => authState.loggedIn)

const loadDraft = async () => {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok && res.status !== 204) {
      const data = await res.json()
      title.value = data.title || ''
      content.value = data.content || ''
      selectedCategory.value = data.categoryId || ''
      selectedTags.value = data.tagIds || []
      postVisibleScope.value = data.visiblescope

      toast.success('草稿已加载')
    }
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadDraft)

const clearPost = async () => {
  title.value = ''
  content.value = ''
  selectedCategory.value = ''
  selectedTags.value = []
  postVisibleScope.value = 'ALL'
  postType.value = 'NORMAL'
  lottery.prizeIcon = ''
  lottery.prizeIconFile = null
  lottery.tempPrizeIcon = ''
  lottery.showPrizeCropper = false
  lottery.prizeName = ''
  lottery.prizeDescription = ''
  lottery.prizeCount = 1
  lottery.pointCost = 0
  lottery.endTime = null
  startTime.value = null
  poll.options = ['', '']
  poll.endTime = null
  poll.multiple = false

  // 删除草稿
  const token = getToken()
  if (token) {
    const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
    if (res.ok) {
      toast.success('草稿已清空')
    } else {
      toast.error('云端草稿清空失败, 请稍后重试')
    }
  }
}

const saveDraft = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const tagIds = selectedTags.value.filter((t) => typeof t === 'number')
    const res = await fetch(`${API_BASE_URL}/api/drafts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title.value,
        content: content.value,
        categoryId: selectedCategory.value || null,
        tagIds,
        postVisibleScopeType:postVisibleScope.value
      }),
    })
    if (res.ok) {
      toast.success('草稿已保存')
    } else {
      toast.error('保存失败')
    }
  } catch (e) {
    toast.error('保存失败')
  }
}
const ensureTags = async (token) => {
  for (let i = 0; i < selectedTags.value.length; i++) {
    const t = selectedTags.value[i]
    if (typeof t === 'string' && t.startsWith('__new__:')) {
      const name = t.slice(8)
      const res = await fetch(`${API_BASE_URL}/api/tags`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ name, description: '' }),
      })
      if (res.ok) {
        const data = await res.json()
        selectedTags.value[i] = data.id
        // update local TagSelect options handled by component
      } else {
        let data
        try {
          data = await res.json()
        } catch (e) {
          data = null
        }
        toast.error((data && data.error) || '创建标签失败')
        throw new Error('create tag failed')
      }
    }
  }
}

const aiGenerate = async () => {
  if (!content.value.trim()) {
    toast.error('内容为空，无法优化')
    return
  }
  isAiLoading.value = true
  try {
    toast.info('AI 优化中...')
    const token = getToken()
    const res = await fetch(`${API_BASE_URL}/api/ai/format`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ text: content.value }),
    })
    if (res.ok) {
      const data = await res.json()
      content.value = data.content || ''
    } else if (res.status === 429) {
      toast.error('今日AI优化次数已用尽')
    } else {
      toast.error('AI 优化失败')
    }
  } catch (e) {
    toast.error('AI 优化失败')
  } finally {
    isAiLoading.value = false
  }
}

const submitPost = async () => {
  if (!title.value.trim()) {
    toast.error('标题不能为空')
    return
  }
  if (!content.value.trim()) {
    toast.error('内容不能为空')
    return
  }
  if (!selectedCategory.value) {
    toast.error('请选择分类')
    return
  }
  if (selectedTags.value.length === 0) {
    toast.error('请选择标签')
    return
  }
  if (postType.value === 'LOTTERY') {
    if (!lottery.prizeIcon) {
      toast.error('请上传奖品图片')
      return
    }
    if (!lottery.prizeCount || lottery.prizeCount < 1) {
      toast.error('奖品数量必须大于0')
      return
    }
    if (!lottery.prizeDescription) {
      toast.error('请输入奖品描述')
      return
    }
    if (!lottery.endTime) {
      toast.error('请选择抽奖结束时间')
      return
    }
    if (lottery.pointCost < 0 || lottery.pointCost > 100) {
      toast.error('参与积分需在0到100之间')
      return
    }
  }
  if (postType.value === 'POLL') {
    if (poll.options.length < 2 || poll.options.some((o) => !o.trim())) {
      toast.error('请填写至少两个投票选项')
      return
    }
    if (!poll.endTime) {
      toast.error('请选择投票结束时间')
      return
    }
  }
  try {
    const token = getToken()
    await ensureTags(token)
    isWaitingPosting.value = true
    let prizeIconUrl = lottery.prizeIcon
    if (postType.value === 'LOTTERY' && lottery.prizeIconFile) {
      const form = new FormData()
      form.append('file', lottery.prizeIconFile)
      const uploadRes = await fetch(`${API_BASE_URL}/api/upload`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      })
      const uploadData = await uploadRes.json()
      if (!uploadRes.ok || uploadData.code !== 0) {
        toast.error('奖品图片上传失败')
        return
      }
      prizeIconUrl = uploadData.data.url
    }
    const res = await fetch(`${API_BASE_URL}/api/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title.value,
        content: content.value,
        categoryId: selectedCategory.value,
        tagIds: selectedTags.value,
        postVisibleScopeType: postVisibleScope.value,
        type: postType.value,
        prizeIcon: postType.value === 'LOTTERY' ? prizeIconUrl : undefined,
        prizeName: postType.value === 'LOTTERY' ? lottery.prizeName : undefined,
        prizeCount: postType.value === 'LOTTERY' ? lottery.prizeCount : undefined,
        prizeDescription: postType.value === 'LOTTERY' ? lottery.prizeDescription : undefined,
        options: postType.value === 'POLL' ? poll.options : undefined,
        multiple: postType.value === 'POLL' ? poll.multiple : undefined,
        startTime:
          postType.value === 'LOTTERY' ? new Date(startTime.value).toISOString() : undefined,
        pointCost: postType.value === 'LOTTERY' ? lottery.pointCost : undefined,
        // 将时间转换为 UTC+8.5 时区 todo: 需要优化
        endTime:
          postType.value === 'LOTTERY'
            ? new Date(new Date(lottery.endTime).getTime() + 8.02 * 60 * 60 * 1000).toISOString()
            : postType.value === 'POLL'
              ? new Date(new Date(poll.endTime).getTime() + 8.02 * 60 * 60 * 1000).toISOString()
              : undefined,
      }),
    })
    const data = await res.json()
    if (res.ok) {
      if (data.reward && data.reward > 0) {
        toast.success(`发布成功，获得 ${data.reward} 经验值`)
      } else {
        toast.success('发布成功')
      }
      if (data.id) {
        window.location.href = `/posts/${data.id}`
      }
    } else if (res.status === 429) {
      toast.error('发布过于频繁，请稍后再试')
    } else {
      toast.error(data.error || '发布失败')
    }
  } catch (e) {
    toast.error('发布失败')
  } finally {
    isWaitingPosting.value = false
  }
}
</script>

<style scoped>
.new-post-page {
  display: flex;
  justify-content: center;
  background-color: var(--background-color);
  padding-right: 20px;
  padding-left: 20px;
}

.new-post-form {
  width: 100%;
}

.post-title-input {
  border: none;
  outline: none;
  padding-top: 20px;
  padding-bottom: 20px;
  background-color: transparent;
  font-size: 42px;
  width: 100%;
  font-weight: bold;
  color: var(--text-color);
}

.post-draft {
  color: var(--primary-color);
  border-radius: 10px;
  cursor: pointer;
}

.post-draft:hover {
  text-decoration: underline;
}

.ai-generate {
  color: var(--primary-color);
  cursor: pointer;
}

.ai-generate:hover {
  text-decoration: underline;
}

.post-clear {
  color: var(--primary-color);
  cursor: pointer;
  opacity: 0.7;
}

.post-editor-container {
  position: relative;
}

.post-submit {
  background-color: var(--primary-color);
  color: #fff;
  padding: 10px 20px;
  border-radius: 10px;
  width: fit-content;
  cursor: pointer;
}

.post-submit.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.post-submit:hover {
  background-color: var(--primary-color-hover);
}

.post-submit.disabled:hover {
  background-color: var(--primary-color-disabled);
}

.post-submit-loading {
  color: white;
  background-color: var(--primary-color-disabled);
  padding: 10px 20px;
  border-radius: 10px;
  width: fit-content;
  cursor: not-allowed;
}

.post-options-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.post-options-right {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 30px;
  row-gap: 10px;
  flex-wrap: wrap;
}

.post-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 20px;
  padding-bottom: 50px;
}

@media (max-width: 768px) {
  .new-post-page {
    width: calc(100vw - 20px);
    padding-right: 10px;
    padding-left: 10px;
    overflow-x: hidden;
  }

  .post-title-input {
    font-size: 24px;
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .post-options {
    margin-top: 10px;
  }
}
</style>
