<template>
  <header class="header">
    <div class="header-content">
      <div class="header-content-left">
        <div v-if="showMenuBtn" class="menu-btn-wrapper">
          <button class="menu-btn" @click="$emit('toggle-menu')">
            <i class="fas fa-bars"></i>
          </button>
          <span v-if="isMobile && unreadCount > 0" class="menu-unread-dot"></span>
        </div>
        <div class="logo-container" @click="goToHome">
          <img alt="OpenIsle" src="https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/image.png"
            width="60" height="60">
          <div class="logo-text">OpenIsle</div>
        </div>
      </div>

      <ClientOnly>
        <div v-if="isLogin" class="header-content-right">
          <div v-if="isMobile" class="search-icon" @click="search">
            <i class="fas fa-search"></i>
          </div>
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
          <div v-if="isMobile" class="search-icon" @click="search">
            <i class="fas fa-search"></i>
          </div>
          <div class="header-content-item-main" @click="goToLogin">登录</div>
          <div class="header-content-item-secondary" @click="goToSignup">注册</div>
        </div>
      </ClientOnly>
      
      <SearchDropdown ref="searchDropdown" v-if="isMobile && showSearch" @close="closeSearch" />
    </div>
  </header>
</template>

<script>
import { authState, clearToken, loadCurrentUser } from '~/utils/auth'
import { watch, nextTick } from 'vue'
import { fetchUnreadCount, notificationState } from '~/utils/notification'
import DropdownMenu from '~/components/DropdownMenu.vue'
import SearchDropdown from '~/components/SearchDropdown.vue'
import { useIsMobile } from '~/utils/screen'
import { ClientOnly } from '#components'

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
      avatar: '',
      showSearch: false,
      searchDropdown: null
    }
  },
  setup() {
    const isLogin = computed(() => authState.loggedIn)
    const isMobile = useIsMobile()
    const unreadCount = computed(() => notificationState.unreadCount)
    const router = useRouter()

    const goToHome = () => {
      router.push('/').then(() => {
        window.location.reload()
      })
    }
    const search = () => {
      showSearch.value = true
      nextTick(() => {
        searchDropdown.value.toggle()
      })
    }
    const closeSearch = () => {
      nextTick(() => {
        showSearch.value = false
      })
    }
    const goToLogin = () => {
      router.push('/login')
    }
    const goToSettings = () => {
      router.push('/settings')
    }
    const goToProfile = async () => {
      if (!authState.loggedIn) {
        router.push('/login')
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
        router.push(`/users/${id}`)
      }
    }
    const goToSignup = () => {
      router.push('/signup')
    }
    const goToLogout = () => {
      clearToken()
      this.$router.push('/login')
    }

    const headerMenuItems = computed(() => [
      { text: '设置', onClick: goToSettings },
      { text: '个人主页', onClick: goToProfile },
      { text: '退出', onClick: goToLogout }
    ])

    return {
      isLogin,
      isMobile,
      headerMenuItems,
      unreadCount,
      goToHome,
      search,
      closeSearch,
      goToLogin,
      goToSettings,
      goToProfile,
      goToSignup,
      goToLogout
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
    const updateUnread = async () => {
      if (authState.loggedIn) {
        await fetchUnreadCount()
      } else {
        notificationState.unreadCount = 0
      }
    }

    await updateAvatar()
    await updateUnread()

    watch(() => authState.loggedIn, async () => {
      await updateAvatar()
      await updateUnread()
    })

    watch(() => this.$route.fullPath, () => {
      if (this.$refs.userMenu) this.$refs.userMenu.close()
      this.showSearch = false
    })
  },
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: var(--header-height);
  background-color: var(--background-color-blur);
  backdrop-filter: blur(10px);
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

.menu-btn-wrapper {
  position: relative;
  display: inline-block;
}

.menu-unread-dot {
  position: absolute;
  top: 0;
  right: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff4d4f;
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
  object-fit: cover;
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

.search-icon {
  font-size: 18px;
  cursor: pointer;
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
