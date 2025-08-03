<template>
  <div class="signup-page">
    <div class="signup-page-content">
      <div class="signup-page-header">
        <div class="signup-page-header-title">
          Welcome :)
        </div>
      </div>

      <div v-if="emailStep === 0" class="email-signup-page-content">
        <BaseInput
          icon="fas fa-envelope"
          v-model="email"
          @input="emailError = ''"
          placeholder="邮箱"
        />
        <div v-if="emailError" class="error-message">{{ emailError }}</div>

        <BaseInput
          icon="fas fa-user"
          v-model="username"
          @input="usernameError = ''"
          placeholder="用户名"
        />
        <div v-if="usernameError" class="error-message">{{ usernameError }}</div>

        <BaseInput
          icon="fas fa-lock"
          v-model="password"
          @input="passwordError = ''"
          type="password"
          placeholder="密码"
        />
        <div v-if="passwordError" class="error-message">{{ passwordError }}</div>


        <div v-if="!isWaitingForEmailSent" class="signup-page-button-primary" @click="sendVerification">
          <div class="signup-page-button-text">验证邮箱</div>
        </div>
        <div v-else class="signup-page-button-primary disabled">
          <div class="signup-page-button-text">
            <i class="fas fa-spinner fa-spin"></i>
            发送中...
          </div>
        </div>

        <div class="signup-page-button-secondary">已经有账号？ <a class="signup-page-button-secondary-link"
            href="/login">登录</a></div>
      </div>

      <div v-if="emailStep === 1" class="email-signup-page-content">
        <BaseInput
          icon="fas fa-envelope"
          v-model="code"
          placeholder="邮箱验证码"
        />
        <div v-if="!isWaitingForEmailVerified" class="signup-page-button-primary" @click="verifyCode">
          <div class="signup-page-button-text">注册</div>
        </div>
        <div v-else class="signup-page-button-primary disabled">
          <div class="signup-page-button-text">
            <i class="fas fa-spinner fa-spin"></i>
            验证中...
          </div>
        </div>
      </div>
    </div>

    <div class="other-signup-page-content">
      <div class="signup-page-button" @click="loginWithGoogleWithPop">
        <img class="signup-page-button-icon" src="../assets/icons/google.svg" alt="Google Logo" />
        <div class="signup-page-button-text">Google 注册</div>
      </div>
      <div class="signup-page-button" @click="signupWithGithub">
        <img class="signup-page-button-icon" src="../assets/icons/github.svg" alt="GitHub Logo" />
        <div class="signup-page-button-text">GitHub 注册</div>
      </div>
      <div class="signup-page-button" @click="signupWithDiscord">
        <img class="signup-page-button-icon" src="../assets/icons/discord.svg" alt="Discord Logo" />
        <div class="signup-page-button-text">Discord 注册</div>
      </div>
      <div class="signup-page-button" @click="signupWithTwitter">
        <img class="signup-page-button-icon" src="../assets/icons/twitter.svg" alt="Twitter Logo" />
        <div class="signup-page-button-text">Twitter 注册</div>
      </div>
    </div>
  </div>
</template>

<script>
import { API_BASE_URL, toast } from '../main'
import { loginWithGoogleWithPop } from '../utils/google'
import { githubAuthorize } from '../utils/github'
import { discordAuthorize } from '../utils/discord'
import { twitterAuthorize } from '../utils/twitter'
import BaseInput from '../components/BaseInput.vue'
export default {
  name: 'SignupPageView',
  components: { BaseInput },
  setup() {
    return { loginWithGoogleWithPop }
  },
  data() {
    return {
      emailStep: 0,
      email: '',
      username: '',
      password: '',
      registerMode: 'DIRECT',
      emailError: '',
      usernameError: '',
      passwordError: '',
      code: '',
      isWaitingForEmailSent: false,
      isWaitingForEmailVerified: false
    }
  },
  async mounted() {
    this.username = this.$route.query.u || ''
    try {
      const res = await fetch(`${API_BASE_URL}/api/config`)
      if (res.ok) {
        const data = await res.json()
        this.registerMode = data.registerMode
      }
    } catch {/* ignore */}
    if (this.$route.query.verify) {
      this.emailStep = 1
    }
  },
  methods: {
    clearErrors() {
      this.emailError = ''
      this.usernameError = ''
      this.passwordError = ''
    },
    async sendVerification() {
      this.clearErrors()
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(this.email)) {
        this.emailError = '邮箱格式不正确'
      }
      if (!this.password || this.password.length < 6) {
        this.passwordError = '密码至少6位'
      }
      if (!this.username) {
        this.usernameError = '用户名不能为空'
      }
      if (this.emailError || this.passwordError || this.usernameError) {
        return
      }
      try {
        this.isWaitingForEmailSent = true
        const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: this.username,
            email: this.email,
            password: this.password
          })
        })
        this.isWaitingForEmailSent = false
        const data = await res.json()
        if (res.ok) {
          this.emailStep = 1
          toast.success('验证码已发送，请查看邮箱')
        } else if (data.field) {
          if (data.field === 'username') this.usernameError = data.error
          if (data.field === 'email') this.emailError = data.error
          if (data.field === 'password') this.passwordError = data.error
        } else {
          toast.error(data.error || '发送失败')
        }
      } catch (e) {
        toast.error('发送失败')
      }
    },
    async verifyCode() {
      try {
        this.isWaitingForEmailVerified = true
        const res = await fetch(`${API_BASE_URL}/api/auth/verify`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            code: this.code,
            username: this.username
          })
        })
        this.isWaitingForEmailVerified = false
        const data = await res.json()
        if (res.ok) {
          if (this.registerMode === 'WHITELIST') {
            this.$router.push('/signup-reason?token=' + data.token)
          } else {
            toast.success('注册成功，请登录')
            this.$router.push('/login')
          }
        } else {
          toast.error(data.error || '注册失败')
        }
      } catch (e) {
        toast.error('注册失败')
      }
    },
    signupWithGithub() {
      githubAuthorize()
    },
    signupWithDiscord() {
      discordAuthorize()
    },
    signupWithTwitter() {
      twitterAuthorize()
    }
  }
}
</script>

<style scoped>
.signup-page {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
  width: 100%;
  background-color: var(--background-color);
}

.signup-page-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: calc(40% - 120px);
  border-right: 1px solid var(--normal-border-color);
  padding-right: 120px;
}

.signup-page-header-title {
  font-family: 'Pacifico', 'Comic Sans MS', cursive, 'Roboto', sans-serif;
  font-size: 42px;
  font-weight: bold;
  width: 100%;
  opacity: 0.75;
}

.signup-page-header {
  font-size: 42px;
  font-weight: bold;
  width: 100%;
}

.email-signup-page-content {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 20px;
}

.signup-page-input {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: calc(100% - 40px);
  padding: 15px 20px;
  border-radius: 10px;
  border: 1px solid var(--normal-border-color);
  gap: 10px;
  margin-bottom: 20px;
}

.signup-page-input-icon {
  opacity: 0.5;
  font-size: 16px;
}

.signup-page-input-text {
  border: none;
  outline: none;
  width: 100%;
  font-size: 16px;
}

.other-signup-page-content {
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 30%;
  gap: 20px;
}

.signup-page-button-primary {
  margin-top: 20px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: calc(100% - 40px);
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
  gap: 10px;
}

.signup-page-button-primary.disabled {
  background-color: var(--primary-color-disabled);
  opacity: 0.5;
  cursor: not-allowed;
}

.signup-page-button-primary.disabled:hover {
  background-color: var(--primary-color-disabled);
}

.signup-page-button-primary:hover {
  background-color: var(--primary-color-hover);
}

.signup-page-button {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  background-color: var(--login-background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  cursor: pointer;
  min-width: 150px;
  gap: 10px;
}

.signup-page-button:hover {
  background-color: var(--login-background-color-hover);
}

.signup-page-button-icon {
  width: 20px;
  height: 20px;
}

.signup-page-button-text {
  font-size: 16px;
}

.signup-page-button-secondary {
  margin-top: 20px;
  font-size: 16px;
  opacity: 0.7;
}

.signup-page-button-secondary-link {
  color: var(--primary-color);
}

.error-message {
  color: red;
  font-size: 14px;
  width: calc(100% - 40px);
  margin-top: -10px;
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .signup-page {
    flex-direction: column;
    justify-content: flex-start;
  }

  .email-signup-page-content {
    margin-top: 20px;
    gap: 20px;
  }

  .signup-page-content {
    margin-top: 20px;
    width: calc(100% - 40px);
    border-right: none;
    padding-left: 20px;
    padding-right: 20px;
  }

  .signup-page-button-primary {
    margin-top: 0px;
  }

  .signup-page-button-secondary {
    margin-top: 0px;
    font-size: 13px;
  }

  .other-signup-page-content {
    margin-top: 20px;
    margin-left: 0px;
    width: calc(100% - 40px);
    gap: 10px;
  }

  .signup-page-button {
    width: calc(100% - 40px);
  }
}
</style>