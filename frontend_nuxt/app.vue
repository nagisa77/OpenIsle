<template>
  <div id="app">
    <div class="header-container">
      <HeaderComponent
        ref="header"
        @toggle-menu="menuVisible = !menuVisible"
        :show-menu-btn="!hideMenu"
      />
    </div>

    <div class="main-container">
      <div class="menu-container" v-click-outside="handleMenuOutside">
        <MenuComponent :visible="!hideMenu && menuVisible" @item-click="menuVisible = false" />
      </div>
      <div class="content" :class="{ 'menu-open': menuVisible && !hideMenu }">
        <NuxtPage keepalive />
      </div>
    </div>
    <GlobalPopups />
  </div>
</template>

<script>
import HeaderComponent from '~/components/HeaderComponent.vue'
import MenuComponent from '~/components/MenuComponent.vue'

import GlobalPopups from '~/components/GlobalPopups.vue'

export default {
  name: 'App',
  components: { HeaderComponent, MenuComponent, GlobalPopups },
  setup() {
    const isMobile = useIsMobile()
    const menuVisible = ref(!isMobile.value)
    const hideMenu = computed(() => {
      return [
        '/login',
        '/signup',
        '/404',
        '/signup-reason',
        '/github-callback',
        '/twitter-callback',
        '/discord-callback',
        '/forgot-password',
        '/google-callback',
      ].includes(useRoute().path)
    })

    const header = useTemplateRef('header')

    onMounted(() => {
      if (typeof window !== 'undefined') {
        menuVisible.value = window.innerWidth > 768
      }
    })

    const handleMenuOutside = (event) => {
      const btn = header.value.$refs.menuBtn
      if (btn && (btn === event.target || btn.contains(event.target))) {
        return // 如果是菜单按钮的点击，不处理关闭
      }

      if (isMobile.value) {
        menuVisible.value = false
      }
    }

    return { menuVisible, hideMenu, handleMenuOutside, header }
  },
}
</script>
<style src="~/assets/global.css"></style>
<style>
.header-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.menu-container {
}

.content {
  /* height: calc(100vh - var(--header-height)); */
  padding-top: var(--header-height);
  flex: 1;
  max-width: 100%;
  transition: max-width 0.3s ease;
  background-color: var(--background-color);
  min-height: calc(100vh - var(--header-height));
}

.content.menu-open {
  max-width: calc(100% - var(--menu-width));
}

.main-container {
  display: flex;
  flex-direction: row;
  max-width: var(--page-max-width);
  margin: 0 auto;
}

@media (max-width: 768px) {
  .content,
  .content.menu-open {
    max-width: 100% !important;
  }
}
</style>
