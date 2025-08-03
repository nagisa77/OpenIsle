<template>
  <div class="login-page">
    <div class="login-page-content">
      <div class="login-page-header">
        <div class="login-page-header-title">
          Welcome :)
        </div>
      </div>

      <div class="email-login-page-content">
        <BaseInput icon="fas fa-envelope" v-model="username" placeholder="邮箱/用户名" />

        <BaseInput icon="fas fa-lock" v-model="password" type="password" placeholder="密码" />


        <div v-if="!isWaitingForLogin" class="login-page-button-primary" @click="submitLogin">
          <div class="login-page-button-text">登录</div>
        </div>

        <div v-else class="login-page-button-primary disabled">
          <div class="login-page-button-text">
            <i class="fas fa-spinner fa-spin"></i>
            登录中...
          </div>
        </div>

        <div class="login-page-button-secondary">没有账号？ <a class="login-page-button-secondary-link" href="/signup">注册</a> |
          <a class="login-page-button-secondary-link" :href="`/forgot-password?email=${username}`">找回密码</a>
        </div>
      </div>
    </div>

    <div class="other-login-page-content">
      <div class="login-page-button" @click="loginWithGoogle">
        <img class="login-page-button-icon" src="../assets/icons/google.svg" alt="Google Logo" />
        <div class="login-page-button-text">Google 登录</div>
      </div>
      <div class="login-page-button" @click="loginWithGithub">
        <img class="login-page-button-icon" src="../assets/icons/github.svg" alt="GitHub Logo" />
        <div class="login-page-button-text">GitHub 登录</div>
      </div>
      <div class="login-page-button" @click="loginWithDiscord">
        <img class="login-page-button-icon" src="../assets/icons/discord.svg" alt="Discord Logo" />
        <div class="login-page-button-text">Discord 登录</div>
      </div>
      <div class="login-page-button" @click="loginWithTwitter">
        <img class="login-page-button-icon" src="../assets/icons/twitter.svg" alt="Twitter Logo" />
        <div class="login-page-button-text">Twitter 登录</div>
      </div>
    </div>
  </div>
</template>

<script>
import { API_BASE_URL, toast } from '../main'
import { setToken, loadCurrentUser } from '../utils/auth'
import { loginWithGoogle } from '../utils/google'
import { githubAuthorize } from '../utils/github'
import { discordAuthorize } from '../utils/discord'
import { twitterAuthorize } from '../utils/twitter'
import BaseInput from '../components/BaseInput.vue'
import { registerPush } from '../utils/push'
export default {
  name: 'LoginPageView',
  components: { BaseInput },
  setup() {
    return { loginWithGoogle }
  }, 
  data() {
    return {
      username: '',
      password: '',
      isWaitingForLogin: false
    }
  },
  methods: {
    async submitLogin() {
      try {
        this.isWaitingForLogin = true
        const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: this.username, password: this.password })
        })
        this.isWaitingForLogin = false
        const data = await res.json()
        if (res.ok && data.token) {
          setToken(data.token)
          await loadCurrentUser()
          toast.success('登录成功')
          registerPush()
          this.$router.push('/')
        } else if (data.reason_code === 'NOT_VERIFIED') {
          toast.info('当前邮箱未验证，已经为您重新发送验证码')
          this.$router.push({ path: '/signup', query: { verify: 1, u: this.username } })
        } else if (data.reason_code === 'IS_APPROVING') {
          toast.info('您的注册正在审批中, 请留意邮件')
          this.$router.push('/')
        } else if (data.reason_code === 'NOT_APPROVED') {
          this.$router.push('/signup-reason?token=' + data.token)
        } else {
          toast.error(data.error || '登录失败')
        }
      } catch (e) {
        toast.error('登录失败')
      }
    },

    loginWithGithub() {
      githubAuthorize()
    },
    loginWithDiscord() {
      discordAuthorize()
    },
    loginWithTwitter() {
      twitterAuthorize()
    },
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
  width: 100%;
  background-color: var(--background-color);
}

.login-page-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: calc(40% - 120px);
  border-right: 1px solid var(--normal-border-color);
  padding-right: 120px;
}

.login-page-header-title {
  font-family: 'Pacifico', 'Comic Sans MS', cursive, 'Roboto', sans-serif;
  font-size: 42px;
  font-weight: bold;
  width: 100%;
  opacity: 0.75;
}

.login-page-header {
  font-size: 42px;
  font-weight: bold;
  width: 100%;
}

.email-login-page-content {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  gap: 20px;
}

.login-page-input {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: calc(100% - 40px);
  padding: 15px 20px;
  border-radius: 10px;
  border: 1px solid #ccc;
  gap: 10px;
  margin-bottom: 20px;
}

.login-page-input-icon {
  opacity: 0.5;
  font-size: 16px;
}

.login-page-input-text {
  border: none;
  outline: none;
  width: 100%;
  font-size: 16px;
}

.other-login-page-content {
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 30%;
  gap: 20px;
}

.login-page-button-primary {
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

.login-page-button-primary:hover {
  background-color: var(--primary-color-hover);
}

.login-page-button-primary.disabled {
  background-color: var(--primary-color-disabled);
  opacity: 0.5;
  cursor: not-allowed;
}

.login-page-button-primary.disabled:hover {
  background-color: var(--primary-color-disabled);
}

.login-page-button {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  min-width: 150px;
  background-color: var(--login-background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  cursor: pointer;
  gap: 10px;
}

.login-page-button:hover {
  background-color: var(--login-background-color-hover);
}

.login-page-button-icon {
  width: 20px;
  height: 20px;
}

.login-page-button-text {
  font-size: 16px;
}

.login-page-button-secondary {
  margin-top: 20px;
  font-size: 16px;
  opacity: 0.7;
}

.login-page-button-secondary-link {
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
    justify-content: flex-start;
  }

  .login-page-content {
    margin-top: 20px;
    width: calc(100% - 40px);
    border-right: none;
    padding-left: 20px;
    padding-right: 20px;
  }

  .login-page-button-primary {
    margin-top: 0px;
  }

  .login-page-button-secondary {
    margin-top: 0px;
    font-size: 13px;
  }

  .other-login-page-content {
    margin-top: 20px;
    margin-left: 0px;
    width: calc(100% - 40px);
    gap: 10px;
  }

  .login-page-button {
    width: calc(100% - 40px);
  }
}
</style>