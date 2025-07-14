<template>
  <div class="settings-page">
    <div v-if="isLoadingPage" class="loading-page">
      <l-hatch size="20" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <div v-else>
      <div class="settings-title">个人资料设置</div>
      <div class="profile-section">
        <div class="avatar-row">
          <!-- label 充当点击区域，内部隐藏 input -->
          <label class="avatar-container">
            <img :src="avatar" class="avatar-preview" />
            <!-- 半透明蒙层：hover 时出现 -->
            <div class="avatar-overlay">更换头像</div>
            <input type="file" class="avatar-input" accept="image/*" @change="onAvatarChange" />
          </label>
        </div>
        <div class="form-row username-row">
          <BaseInput icon="fas fa-user" v-model="username" @input="usernameError = ''" placeholder="用户名" />
          <div class="setting-description">用户名是你在社区的唯一标识</div>
          <div v-if="usernameError" class="error-message">{{ usernameError }}</div>
        </div>
        <div class="form-row introduction-row">
          <div class="setting-title">自我介绍</div>
          <BaseInput v-model="introduction" textarea rows="3" placeholder="说些什么..." />
          <div class="setting-description">自我介绍会出现在你的个人主页，可以简要介绍自己</div>
        </div>
      </div>
      <div v-if="role === 'ADMIN'" class="admin-section">
        <h3>管理员设置</h3>
        <div class="form-row dropdown-row">
          <div class="setting-title">发布规则</div>
          <Dropdown v-model="publishMode" :fetch-options="fetchPublishModes" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">密码强度</div>
          <Dropdown v-model="passwordStrength" :fetch-options="fetchPasswordStrengths" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">AI 优化次数</div>
          <Dropdown v-model="aiFormatLimit" :fetch-options="fetchAiLimits" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">注册模式</div>
          <Dropdown v-model="registerMode" :fetch-options="fetchRegisterModes" />
        </div>
      </div>
      <div class="buttons">
        <div v-if="isSaving" class="save-button disabled">保存中...</div>
        <div v-else @click="save" class="save-button">保存</div>
      </div>
    </div>
  </div>
</template>

<script>
import { API_BASE_URL, toast } from '../main'
import { getToken, fetchCurrentUser } from '../utils/auth'
import BaseInput from '../components/BaseInput.vue'
import Dropdown from '../components/Dropdown.vue'
import { hatch } from 'ldrs'
hatch.register()
export default {
  name: 'SettingsPageView',
  components: { BaseInput, Dropdown },
  data() {
    return {
      username: '',
      introduction: '',
      usernameError: '',
      avatar: '',
      avatarFile: null,
      role: '',
      publishMode: 'DIRECT',
      passwordStrength: 'LOW',
      aiFormatLimit: 3,
      registerMode: 'DIRECT',
      isLoadingPage: false,
      isSaving: false
    }
  },
  async mounted() {
    this.isLoadingPage = true
    const user = await fetchCurrentUser()

    if (user) {
      this.username = user.username
      this.introduction = user.introduction || ''
      this.avatar = user.avatar
      this.role = user.role
      if (this.role === 'ADMIN') {
        this.loadAdminConfig()
      }
    }
    this.isLoadingPage = false
  },
  methods: {
    onAvatarChange(e) {
      const file = e.target.files[0]
      this.avatarFile = file
      if (file) {
        const reader = new FileReader()
        reader.onload = () => {
          this.avatar = reader.result
        }
        reader.readAsDataURL(file)
      }
    },
    fetchPublishModes() {
      return Promise.resolve([
        { id: 'DIRECT', name: '直接发布', icon: 'fas fa-bolt' },
        { id: 'REVIEW', name: '审核后发布', icon: 'fas fa-search' }
      ])
    },
    fetchPasswordStrengths() {
      return Promise.resolve([
        { id: 'LOW', name: '低', icon: 'fas fa-lock-open' },
        { id: 'MEDIUM', name: '中', icon: 'fas fa-lock' },
        { id: 'HIGH', name: '高', icon: 'fas fa-user-shield' }
      ])
    },
    fetchAiLimits() {
      return Promise.resolve([
        { id: 3, name: '3次' },
        { id: 5, name: '5次' },
        { id: 10, name: '10次' },
        { id: -1, name: '无限' }
      ])
    },
    fetchRegisterModes() {
      return Promise.resolve([
        { id: 'DIRECT', name: '直接注册', icon: 'fas fa-user-check' },
        { id: 'WHITELIST', name: '白名单邀请制', icon: 'fas fa-envelope' }
      ])
    },
    async loadAdminConfig() {
      try {
        const token = getToken()
        const res = await fetch(`${API_BASE_URL}/api/admin/config`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (res.ok) {
          const data = await res.json()
          this.publishMode = data.publishMode
          this.passwordStrength = data.passwordStrength
          this.aiFormatLimit = data.aiFormatLimit
          this.registerMode = data.registerMode
        }
      } catch (e) {
        // ignore
      }
    },
    async save() {
      this.isSaving = true

      do {
        const token = getToken()
        this.usernameError = ''
        if (!this.username) {
          this.usernameError = '用户名不能为空'
        } else if (this.username.length < 6) {
          this.usernameError = '用户名至少6位'
        }
        if (this.usernameError) {
          toast.error(this.usernameError)
          break
        }
        if (this.avatarFile) {
          const form = new FormData()
          form.append('file', this.avatarFile)
          const res = await fetch(`${API_BASE_URL}/api/users/me/avatar`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}` },
            body: form
          })
          const data = await res.json()
          if (res.ok) {
            this.avatar = data.url
          } else {
            toast.error(data.error || '上传失败')
            break
          }
        }
        const res = await fetch(`${API_BASE_URL}/api/users/me`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
          body: JSON.stringify({ username: this.username, introduction: this.introduction })
        })
        if (!res.ok) {
          const data = await res.json()
          toast.error(data.error || '保存失败')
          break
        }
        if (this.role === 'ADMIN') {
          await fetch(`${API_BASE_URL}/api/admin/config`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
            body: JSON.stringify({ publishMode: this.publishMode, passwordStrength: this.passwordStrength, aiFormatLimit: this.aiFormatLimit, registerMode: this.registerMode })
          })
        }
        toast.success('保存成功')
      } while (!this.isSaving)

      this.isSaving = false
    },
  }
}
</script>

<style scoped>
.loading-page {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 300px;
}

.settings-page {
  background-color: var(--background-color);
  padding: 40px;
  height: calc(100vh - var(--header-height) - 80px);
  overflow-y: auto;
}

.settings-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 40px;
  object-fit: cover;
}

.form-row {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.username-row {
  max-width: 300px;
}

.admin-section {
  margin-top: 30px;
  padding-top: 10px;
}

.setting-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.setting-description {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.introduction-row {
  max-width: 500px;
}

.dropdown-row {
  max-width: 200px;
}

.profile-section {
  margin-bottom: 30px;
}

.buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

/* 容器：定位 + 光标 */
.avatar-container {
  position: relative;
  display: inline-block;
  cursor: pointer;
  background-color: lightgray;
  border-radius: 50%;
}

/* 隐藏默认文件选择按钮 */
.avatar-input {
  display: none;
}

/* 蒙层：初始透明，hover 时渐显 */
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 40px;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
}

/* hover 触发 */
.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.error-message {
  color: red;
  font-size: 14px;
  width: calc(100% - 40px);
  margin-top: -10px;
  margin-bottom: 10px;
}

.save-button {
  margin-top: 40px;
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s ease-in-out;
}

.save-button:hover {
  background-color: var(--primary-color-hover);
}

.save-button.disabled:hover {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.save-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}
</style>
