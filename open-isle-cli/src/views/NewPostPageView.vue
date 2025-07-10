<template>
  <div class="new-post-page">
    <div class="new-post-form">
      <input class="post-title-input" v-model="title" placeholder="标题" />
      <div class="post-editor-container">
        <PostEditor v-model="content" />
      </div>
      <div class="post-options">
        <div class="post-options-left">
          <CategorySelect v-model="selectedCategory" />
          <TagSelect v-model="selectedTags" creatable />
        </div>
        <div class="post-options-right">
          <div class="post-clear" @click="clearPost">
            <i class="fa-solid fa-eraser"></i> 清空
          </div>
          <div class="post-draft" @click="saveDraft">存草稿</div>
          <div v-if="!isWaitingPosting" class="post-submit" @click="submitPost">发布</div>
          <div v-else class="post-submit-loading"> <i class="fa-solid fa-spinner fa-spin"></i> 发布中...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import PostEditor from '../components/PostEditor.vue'
import CategorySelect from '../components/CategorySelect.vue'
import TagSelect from '../components/TagSelect.vue'
import { API_BASE_URL, toast } from '../main'
import { getToken } from '../utils/auth'

export default {
  name: 'NewPostPageView',
  data() {
    return {
      isWaitingPosting: false
    }
  },
  components: { PostEditor, CategorySelect, TagSelect },
  setup() {
    const title = ref('')
    const content = ref('')
    const selectedCategory = ref('')
    const selectedTags = ref([])

    const loadDraft = async () => {
      const token = getToken()
      if (!token) return
      try {
        const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
          headers: { Authorization: `Bearer ${token}` }
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

      // 删除草稿
      const token = getToken()
      if (token) {
        const res = await fetch(`${API_BASE_URL}/api/drafts/me`, {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`
          }
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
        const res = await fetch(`${API_BASE_URL}/api/drafts`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({
            title: title.value,
            content: content.value,
            categoryId: selectedCategory.value || null,
            tagIds: selectedTags.value
          })
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
              Authorization: `Bearer ${token}`
            },
            body: JSON.stringify({ name, description: '' })
          })
          if (res.ok) {
            const data = await res.json()
            selectedTags.value[i] = data.id
            // update local TagSelect options handled by component
          } else {
            throw new Error('create tag failed')
          }
        }
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
      try {
        const token = getToken()
        await ensureTags(token)
        const res = await fetch(`${API_BASE_URL}/api/posts`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({
            title: title.value,
            content: content.value,
            categoryId: selectedCategory.value,
            tagIds: selectedTags.value
          })
        })
        const data = await res.json()
        if (res.ok) {
          toast.success('发布成功')
          if (data.id) {
            window.location.href = `/posts/${data.id}`
          }
        } else {
          toast.error(data.error || '发布失败')
        }
      } catch (e) {
        toast.error('发布失败')
      }
    }
    return { title, content, selectedCategory, selectedTags, submitPost, saveDraft, clearPost }
  }
}
</script>

<style scoped>
.new-post-page {
  display: flex;
  justify-content: center;
  background-color: var(--background-color);
  height: calc(100vh - var(--header-height));
  padding-right: 20px;
  padding-left: 20px;
}

.new-post-form {
  width: 100%;
}

.post-title-input {
  font-size: 18px;
  border: none;
  outline: none;
  padding-top: 20px;
  padding-bottom: 20px;
  background-color: transparent;
  font-size: 42px;
  width: 100%;
  font-weight: bold;
}

.post-draft {
  color: var(--primary-color);
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: underline;
}

.post-clear {
  color: var(--primary-color);
  cursor: pointer;
  opacity: 0.7;
}

.post-submit {
  background-color: var(--primary-color);
  color: #fff;
  padding: 10px 20px;
  border-radius: 10px;
  width: fit-content;
  cursor: pointer;
}

.post-submit:hover {
  background-color: var(--primary-color-hover);
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
}

.post-options-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.post-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

</style>

