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
        <SearchDropdown v-if="isMobile" />
        <DropdownMenu ref="userMenu" :items="headerMenuItems">
          <template #trigger>
            <div class="avatar-container">
              <img class="avatar-img" :src="avatar" alt="avatar">
              <i class="fas fa-caret-down dropdown-icon"></i>
            </div>
          </template>
        </DropdownMenu>
      </div>

      <div v-else class="header-content-right">
        <SearchDropdown v-if="isMobile" />
        <div class="header-content-item-main" @click="goToLogin">登录</div>
        <div class="header-content-item-secondary" @click="goToSignup">注册</div>
      </div>
    </div>
  </header>
</template>

<script>
import { authState, clearToken, loadCurrentUser } from '../utils/auth'
import { watch } from 'vue'
import DropdownMenu from './DropdownMenu.vue'
import SearchDropdown from './SearchDropdown.vue'
import { isMobile } from '../utils/screen'

export default {
  name: 'HeaderComponent',
  components: { DropdownMenu, SearchDropdown },
  props: {
    showMenuBtn: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      avatar: ''
    }
  },
  computed: {
    isLogin() {
      return authState.loggedIn
    },
    isMobile() {
      return isMobile.value
    },
    headerMenuItems() {
      return [
        { text: '设置', onClick: this.goToSettings },
        { text: '个人主页', onClick: this.goToProfile },
        { text: '退出', onClick: this.goToLogout }
      ]
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
      if (this.$refs.userMenu) this.$refs.userMenu.close()
    })
  },


  methods: {
    goToHome() {
      this.$router.push('/').then(() => {
        window.location.reload()
      })
    },
    goToLogin() {
      this.$router.push('/login')
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
        this.$router.push(`/users/${id}`).then(() => {
          window.location.reload()
        })
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

.dropdown-item {
  padding: 8px 16px;
  white-space: nowrap;
}

.dropdown-item:hover {
  background-color: var(--menu-selected-background-color);
}

@media (max-width: 1200px) {
  .header-content {
    padding-left: 15px;
    padding-right: 15px;
    width: calc(100% - 30px);
  }
}

@media (max-width: 768px) {
  .header-content-item-secondary {
    display: none;
  }

  .logo-text {
    display: none;
  }
}

</style>
