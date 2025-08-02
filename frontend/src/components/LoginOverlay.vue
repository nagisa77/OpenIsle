<template>
  <div class="login-overlay">
    <div class="login-overlay-blur"></div>
    <div class="login-overlay-content">
      <i class="fa-solid fa-user login-overlay-icon"></i>
      <div class="login-overlay-text">
        请先登录，点击跳转到登录页面
      </div>
      <div class="login-overlay-button" @click="goLogin">
        登录
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'LoginOverlay',
  setup() {
    const router = useRouter()
    const goLogin = () => {
      router.push('/login')
    }
    return { goLogin }
  }
}
</script>

<style scoped>
.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 15;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.login-overlay-blur {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(3px);
  z-index: 1;
}

.login-overlay-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  border-radius: 10px;
  padding: 24px 32px;
  flex-wrap: wrap;
  justify-content: center;
  row-gap: 20px;
}

.login-overlay-icon {
  margin-right: 10px;
}

.login-overlay-button {
  padding: 4px 12px;
  border-radius: 5px;
  background-color: var(--primary-color);
  color: white;
  cursor: pointer;
  margin-left: 10px;
  cursor: pointer;
}

.login-overlay-button:hover {
  background-color: var(--primary-color-hover);
}

</style>
