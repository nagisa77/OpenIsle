<template>
  <div class="forgot-page">
    <div class="forgot-content">
      <div class="forgot-title">找回密码</div>
      <div v-if="step === 0" class="step-content">
        <BaseInput icon="fas fa-envelope" v-model="email" placeholder="邮箱" />
        <div v-if="emailError" class="error-message">{{ emailError }}</div>
        <div class="primary-button" @click="sendCode" v-if="!isSending">发送验证码</div>
        <div class="primary-button disabled" v-else>发送中...</div>
      </div>
      <div v-else-if="step === 1" class="step-content">
        <BaseInput icon="fas fa-envelope" v-model="code" placeholder="邮箱验证码" />
        <div class="primary-button" @click="verifyCode" v-if="!isVerifying">验证</div>
        <div class="primary-button disabled" v-else>验证中...</div>
      </div>
      <div v-else class="step-content">
        <BaseInput icon="fas fa-lock" v-model="password" type="password" placeholder="新密码" />
        <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        <div class="primary-button" @click="resetPassword" v-if="!isResetting">重置密码</div>
        <div class="primary-button disabled" v-else>提交中...</div>
      </div>
    </div>
  </div>
</template>

<script>
import { API_BASE_URL, toast } from '../main'
import BaseInput from '../components/BaseInput.vue'
export default {
  name: 'ForgotPasswordPageView',
  components: { BaseInput },
  data() {
    return {
      step: 0,
      email: '',
      code: '',
      password: '',
      token: '',
      emailError: '',
      passwordError: '',
      isSending: false,
      isVerifying: false,
      isResetting: false
    }
  },
  mounted() {
    if (this.$route.query.email) {
      this.email = decodeURIComponent(this.$route.query.email)
    }
  },
  methods: {
    async sendCode() {
      if (!this.email) {
        this.emailError = '邮箱不能为空'
        return
      }
      try {
        this.isSending = true
        const res = await fetch(`${API_BASE_URL}/api/auth/forgot/send`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: this.email })
        })
        this.isSending = false
        if (res.ok) {
          toast.success('验证码已发送')
          this.step = 1
        } else {
          toast.error('请填写已注册邮箱')
        }
      } catch (e) {
        this.isSending = false
        toast.error('发送失败')
      }
    },
    async verifyCode() {
      try {
        this.isVerifying = true
        const res = await fetch(`${API_BASE_URL}/api/auth/forgot/verify`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: this.email, code: this.code })
        })
        this.isVerifying = false
        const data = await res.json()
        if (res.ok) {
          this.token = data.token
          this.step = 2
        } else {
          toast.error(data.error || '验证失败')
        }
      } catch (e) {
        this.isVerifying = false
        toast.error('验证失败')
      }
    },
    async resetPassword() {
      if (!this.password) {
        this.passwordError = '密码不能为空'
        return
      }
      try {
        this.isResetting = true
        const res = await fetch(`${API_BASE_URL}/api/auth/forgot/reset`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token: this.token, password: this.password })
        })
        this.isResetting = false
        const data = await res.json()
        if (res.ok) {
          toast.success('密码已重置')
          this.$router.push('/login')
        } else if (data.field === 'password') {
          this.passwordError = data.error
        } else {
          toast.error(data.error || '重置失败')
        }
      } catch (e) {
        this.isResetting = false
        toast.error('重置失败')
      }
    }
  }
}
</script>

<style scoped>
.forgot-page {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background-color);
  height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
}
.forgot-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 400px;
}
.forgot-title {
  font-size: 24px;
  font-weight: bold;
}
.step-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.primary-button {
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
}
.primary-button:hover {
  background-color: var(--primary-color-hover);
}
.primary-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}
.error-message {
  color: red;
  font-size: 14px;
}
@media (max-width: 768px) {
  .forgot-content {
    width: calc(100vw - 40px);
  }
}
</style>
