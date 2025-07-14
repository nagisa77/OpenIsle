<template>
  <div class="reason-page">
    <div class="reason-content">
      <BaseInput textarea rows="4" v-model="reason" placeholder="请填写注册理由" />
      <div v-if="error" class="error-message">{{ error }}</div>
      <div class="signup-page-button-primary" @click="submit" >提交</div>
    </div>
  </div>
</template>

<script>
import BaseInput from '../components/BaseInput.vue'
import { API_BASE_URL, toast } from '../main'
import { googleSignIn } from '../utils/google'

export default {
  name: 'SignupReasonPageView',
  components: { BaseInput },
  data() {
    return {
      reason: '',
      error: '',
      isGoogle: false
    }
  },
  mounted() {
    this.isGoogle = this.$route.query.google === '1'
    if (!this.isGoogle) {
      if (!sessionStorage.getItem('signup_username')) {
        this.$router.push('/signup')
      }
    }
  },
  methods: {
    async submit() {
      if (!this.reason || this.reason.length < 20) {
        this.error = '请至少输入20个字'
        return
      }
      if (this.isGoogle) {
        googleSignIn(() => { this.$router.push('/') }, this.reason)
        return
      }
      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: sessionStorage.getItem('signup_username'),
            email: sessionStorage.getItem('signup_email'),
            password: sessionStorage.getItem('signup_password'),
            reason: this.reason
          })
        })
        const data = await res.json()
        if (res.ok) {
          toast.success('验证码已发送，请查收邮箱')
          this.$router.push('/signup?verify=1')
        } else {
          toast.error(data.error || '发送失败')
        }
      } catch (e) {
        toast.error('发送失败')
      }
    }
  }
}
</script>

<style scoped>
.reason-page { display: flex; justify-content: center; align-items: center; height: calc(100vh - var(--header-height)); }
.reason-content { display: flex; flex-direction: column; gap: 20px; width: 400px; }
.error-message { color: red; font-size: 14px; }
.signup-page-button-primary { background-color: var(--primary-color); color: white; padding: 10px 20px; border-radius: 10px; text-align: center; cursor: pointer; }
.signup-page-button-primary:hover { background-color: var(--primary-color-hover); }
</style>
