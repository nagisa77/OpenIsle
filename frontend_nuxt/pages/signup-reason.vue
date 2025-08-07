<template>
  <div class="reason-page">
    <div class="reason-content">
      <div class="reason-title">请填写注册理由</div>
      <div class="reason-description">
        为了我们社区的良性发展，请填写注册理由，我们将根据你的理由审核你的注册, 谢谢!
      </div>
      <div class="reason-input-container">
        <BaseInput textarea rows="4" v-model="reason" placeholder="20个字以上"></BaseInput>
        <div class="char-count">{{ reason.length }}/20</div>
      </div>
      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="!isWaitingForRegister" class="signup-page-button-primary" @click="submit">提交</div>
      <div v-else class="signup-page-button-primary disabled">提交中...</div>
    </div>
  </div>
</template>

<script>
import BaseInput from '../components/BaseInput.vue'
import {API_BASE_URL, toast} from '../main'

export default {
  name: 'SignupReasonPageView',
  components: {BaseInput},
  data() {
    return {
      reason: '',
      error: '',
      isWaitingForRegister: false,
      token: '',
    }
  },
  mounted() {
    this.token = this.$route.query.token || ''
    if (!this.token) {
      this.$router.push('/signup')
    }
  },
  methods: {
    async submit() {
      if (!this.reason || this.reason.trim().length < 20) {
        this.error = '请至少输入20个字'
        return
      }

      try {
        this.isWaitingForRegister = true
        const res = await fetch(`${API_BASE_URL}/api/auth/reason`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            token: this.token,
            reason: this.reason
          })
        })
        this.isWaitingForRegister = false
        const data = await res.json()
        if (res.ok) {
          toast.success('注册理由已提交,请等待审核')
          this.$router.push('/')
        } else if (data.reason_code === 'INVALID_CREDENTIALS') {
          toast.error('登录已过期,请重新登录')
          this.$router.push('/login')
        } else {
          toast.error(data.error || '提交失败')
        }
      } catch (e) {
        this.isWaitingForRegister = false
        toast.error('提交失败')
      }
    }
  }
}
</script>

<style scoped>
.reason-page {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background-color);
  height: 100%;
}

.reason-title {
  font-size: 24px;
  font-weight: bold;
}

.reason-description {
  font-size: 14px;
}

.reason-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 400px;
}

.char-count {
  font-size: 12px;
  color: #888;
  width: 100%;
  text-align: right;
}

.error-message {
  color: red;
  font-size: 14px;
}

.signup-page-button-primary {
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
}

.signup-page-button-primary:hover {
  background-color: var(--primary-color-hover);
}

.signup-page-button-primary.disabled {
  background-color: var(--primary-color-disabled);
}

.signup-page-button-primary.disabled:hover {
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .reason-content {
    width: calc(100vw - 40px);
  }
}
</style>
