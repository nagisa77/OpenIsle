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
        </div>
        <div class="post-options-right">
          <div class="post-clear" @click="clearPost"><i class="fa-solid fa-eraser"></i> 清空</div>
          <div class="ai-generate" @click="aiGenerate">
            <i class="fa-solid fa-robot"></i>
            md格式优化
          </div>
          <div class="post-draft" @click="saveDraft">
            <i class="fa-solid fa-floppy-disk"></i>
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
            <i class="fa-solid fa-spinner fa-spin"></i> 发布中...
          </div>
        </div>
      </div>
      <div v-if="postType === 'LOTTERY'" class="lottery-section">
        <AvatarCropper
          :src="tempPrizeIcon"
          :show="showPrizeCropper"
          @close="showPrizeCropper = false"
          @crop="onPrizeCropped"
        />
        <div class="prize-row">
          <span class="prize-row-title">奖品图片</span>
          <label class="prize-container">
            <img v-if="prizeIcon" :src="prizeIcon" class="prize-preview" alt="prize" />
            <i v-else class="fa-solid fa-image default-prize-icon"></i>
            <div class="prize-overlay">上传奖品图片</div>
            <input type="file" class="prize-input" accept="image/*" @change="onPrizeIconChange" />
          </label>
        </div>
        <div class="prize-name-row">
          <span class="prize-row-title">奖品描述</span>
          <BaseInput v-model="prizeDescription" placeholder="奖品描述" />
        </div>
        <div class="prize-count-row">
          <span class="prize-row-title">奖品数量</span>
          <div class="prize-count-input">
            <input
              class="prize-count-input-field"
              type="number"
              v-model.number="prizeCount"
              min="1"
            />
          </div>
        </div>
        <div class="prize-time-row">
          <span class="prize-row-title">抽奖结束时间</span>
          <client-only>
            <flat-pickr v-model="endTime" :config="dateConfig" class="time-picker" />
          </client-only>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import 'flatpickr/dist/flatpickr.css'
import { computed, onMounted, ref, watch } from 'vue'
import FlatPickr from 'vue-flatpickr-component'
import AvatarCropper from '~/components/AvatarCropper.vue'
import BaseInput from '~/components/BaseInput.vue'
import CategorySelect from '~/components/CategorySelect.vue'
import LoginOverlay from '~/components/LoginOverlay.vue'
import PostEditor from '~/components/PostEditor.vue'
import PostTypeSelect from '~/components/PostTypeSelect.vue'
import TagSelect from '~/components/TagSelect.vue'
import { API_BASE_URL, toast } from '~/main'
import { authState, getToken } from '~/utils/auth'

export default {
  name: 'NewPostPageView',
  components: {
    PostEditor,
    CategorySelect,
    TagSelect,
    LoginOverlay,
    PostTypeSelect,
    AvatarCropper,
    FlatPickr,
  },
  setup() {
    const title = ref('')
    const content = ref('')
    const selectedCategory = ref('')
    const selectedTags = ref([])
    const postType = ref('NORMAL')
    const prizeIcon = ref('')
    const prizeIconFile = ref(null)
    const tempPrizeIcon = ref('')
    const showPrizeCropper = ref(false)
    const prizeName = ref('')
    const prizeCount = ref(1)
    const prizeDescription = ref('')
    const endTime = ref(null)
    const startTime = ref(null)
    const dateConfig = { enableTime: true, time_24hr: true, dateFormat: 'Y-m-d H:i' }
    const isWaitingPosting = ref(false)
    const isAiLoading = ref(false)
    const isLogin = computed(() => authState.loggedIn)

    const onPrizeIconChange = (e) => {
      const file = e.target.files[0]
      if (file) {
        const reader = new FileReader()
        reader.onload = () => {
          tempPrizeIcon.value = reader.result
          showPrizeCropper.value = true
        }
        reader.readAsDataURL(file)
      }
    }

    const onPrizeCropped = ({ file, url }) => {
      prizeIconFile.value = file
      prizeIcon.value = url
    }

    watch(prizeCount, (val) => {
      if (!val || val < 1) prizeCount.value = 1
    })

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
      postType.value = 'NORMAL'
      prizeIcon.value = ''
      prizeIconFile.value = null
      tempPrizeIcon.value = ''
      showPrizeCropper.value = false
      prizeDescription.value = ''
      prizeCount.value = 1
      endTime.value = null
      startTime.value = null

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
        if (!prizeIcon.value) {
          toast.error('请上传奖品图片')
          return
        }
        if (!prizeCount.value || prizeCount.value < 1) {
          toast.error('奖品数量必须大于0')
          return
        }
        if (!prizeDescription.value) {
          toast.error('请输入奖品描述')
          return
        }
        if (!endTime.value) {
          toast.error('请选择抽奖结束时间')
          return
        }
      }
      try {
        const token = getToken()
        await ensureTags(token)
        isWaitingPosting.value = true
        let prizeIconUrl = prizeIcon.value
        if (postType.value === 'LOTTERY' && prizeIconFile.value) {
          const form = new FormData()
          form.append('file', prizeIconFile.value)
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
            type: postType.value,
            prizeIcon: postType.value === 'LOTTERY' ? prizeIconUrl : undefined,
            prizeName: postType.value === 'LOTTERY' ? prizeName.value : undefined,
            prizeCount: postType.value === 'LOTTERY' ? prizeCount.value : undefined,
            prizeDescription: postType.value === 'LOTTERY' ? prizeDescription.value : undefined,
            startTime:
              postType.value === 'LOTTERY' ? new Date(startTime.value).toISOString() : undefined,
            // 将时间转换为 UTC+8.5 时区 todo: 需要优化
            endTime:
              postType.value === 'LOTTERY'
                ? new Date(new Date(endTime.value).getTime() + 8.02 * 60 * 60 * 1000).toISOString()
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
    return {
      title,
      content,
      selectedCategory,
      selectedTags,
      postType,
      prizeIcon,
      prizeCount,
      endTime,
      submitPost,
      saveDraft,
      clearPost,
      isWaitingPosting,
      aiGenerate,
      isAiLoading,
      isLogin,
      onPrizeIconChange,
      onPrizeCropped,
      showPrizeCropper,
      tempPrizeIcon,
      dateConfig,
      prizeName,
      prizeDescription,
    }
  },
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

.lottery-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.prize-row-title {
  font-size: 16px;
  color: var(--text-color);
  font-weight: bold;
  margin-bottom: 10px;
}

.prize-row {
  display: flex;
  flex-direction: column;
}

.prize-name-row {
  display: flex;
  flex-direction: column;
}

.prize-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background-color: var(--lottery-background-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.default-prize-icon {
  font-size: 30px;
  opacity: 0.1;
  color: var(--text-color);
}

.prize-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.prize-input {
  display: none;
}

.prize-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.prize-container:hover .prize-overlay {
  opacity: 1;
}

.prize-count-row,
.prize-time-row {
  display: flex;
  flex-direction: column;
}

.prize-count-input {
  display: flex;
  align-items: center;
}

.prize-name-input {
  height: 30px;
  border-radius: 5px;
  border: 1px solid var(--border-color);
  padding: 0 10px;
  margin-left: 10px;
  font-size: 16px;
  color: var(--text-color);
}

.prize-count-input-field {
  width: 50px;
  height: 30px;
  border-radius: 5px;
  border: 1px solid var(--border-color);
  padding: 0 10px;
  font-size: 16px;
  color: var(--text-color);
  background-color: var(--lottery-background-color);
}

.time-picker {
  max-width: 200px;
  height: 30px;
  background-color: var(--lottery-background-color);
  border-radius: 5px;
  border: 1px solid var(--border-color);
  padding: 0 10px;
  font-size: 16px;
  color: var(--text-color);
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
