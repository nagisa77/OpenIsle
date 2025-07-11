<template>
  <header class="header">
    <div class="header-content">
      <div class="header-content-left">
        <button v-if="showMenuBtn" class="menu-btn" @click="$emit('toggle-menu')">
          <i class="fas fa-bars"></i>
        </button>
        <div class="logo-container" @click="goToHome">
          <img alt="OpenIsle" src="https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png"
            width="60" height="60">
          <div class="logo-text">OpenIsle</div>
        </div>
      </div>

      <div v-if="isLogin" class="header-content-right">
        <Dropdown v-model="selectedAction" :fetch-options="fetchOptions" :show-search="false" menu-min-width="auto" menu-class="avatar-dropdown-menu">
          <template #display="{ toggle }">
            <div class="avatar-container" @click="toggle">
              <img class="avatar-img" :src="avatar" alt="avatar">
              <i class="fas fa-caret-down dropdown-icon"></i>
            </div>
          </template>
          <template #option="{ option }">
            <div class="dropdown-item">{{ option.name }}</div>
          </template>
        </Dropdown>
      </div>

      <div v-else class="header-content-right">
        <div class="header-content-item-main" @click="goToLogin">登录</div>
        <div class="header-content-item-secondary" @click="goToSignup">注册</div>
      </div>
    </div>
  </header>
</template>

<script>
import { authState, clearToken, loadCurrentUser } from '../utils/auth'
import { watch } from 'vue'
import Dropdown from './Dropdown.vue'

export default {
  name: 'HeaderComponent',
  components: { Dropdown },
  props: {
    showMenuBtn: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      avatar: '',
      selectedAction: null
    }
  },
  computed: {
    isLogin() {
      return authState.loggedIn
    }
  },
  async mounted() {
    const updateAvatar = async () => {
      if (authState.loggedIn) {
        const user = await loadCurrentUser()
        if (user && user.avatar) {
          this.avatar = user.avatar
        }
      }
    }

    await updateAvatar()

    watch(() => authState.loggedIn, async () => {
      await updateAvatar()
    })

    watch(() => this.$route.fullPath, () => {
      this.selectedAction = null
    })

    watch(() => this.selectedAction, val => {
      if (!val) return
      if (val === 'settings') this.goToSettings()
      else if (val === 'profile') this.goToProfile()
      else if (val === 'logout') this.goToLogout()
      this.selectedAction = null
    })
  },

  beforeUnmount() {},

  methods: {
    goToHome() {
      this.$router.push('/')
    },
    goToLogin() {
      this.$router.push('/login')
    },
    fetchOptions() {
      return Promise.resolve([
        { id: 'settings', name: '设置' },
        { id: 'profile', name: '个人主页' },
        { id: 'logout', name: '退出' }
      ])
    },
    goToSettings() {
      this.$router.push('/settings')
    },
    async goToProfile() {
      if (!authState.loggedIn) {
        this.$router.push('/login')
        return
      }
      let id = authState.username || authState.userId
      if (!id) {
        const user = await loadCurrentUser()
        if (user) {
          id = user.username || user.id
        }
      }
      if (id) {
        this.$router.push(`/users/${id}`)
      }
    },
    goToSignup() {
      this.$router.push('/signup')
    },
    goToLogout() {
      clearToken()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: var(--header-height);
  background-color: var(--header-background-color);
  color: var(--header-text-color);
  border-bottom: 1px solid var(--header-border-color);
}

.logo-container {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
}

.header-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  margin: 0 auto;
  max-width: var(--page-max-width);
}

.header-content-left {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.header-content-right {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 20px;
}

.menu-btn {
  font-size: 24px;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  opacity: 0.4;
  margin-right: 10px;
}

.menu-btn:hover {
  opacity: 0.8;
}

.header-content-item-main {
  background-color: var(--primary-color);
  color: white;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
}

.header-content-item-main:hover {
  background-color: var(--primary-color-hover);
}

.header-content-item-secondary {
  color: var(--primary-color);
  text-decoration: underline;
  cursor: pointer;
}

.avatar-container {
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: lightgray;
}

.dropdown-icon {
  margin-left: 5px;
}

.avatar-dropdown-menu {
  position: absolute;
  top: 40px;
  right: 0;
  background-color: var(--menu-background-color);
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.dropdown-item {
  padding: 8px 16px;
  white-space: nowrap;
}

.dropdown-item:hover {
  background-color: var(--menu-selected-background-color);
}

</style>
