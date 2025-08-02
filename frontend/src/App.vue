<template>
  <div id="app">
    <div class="header-container">
      <HeaderComponent
        @toggle-menu="menuVisible = !menuVisible"
        :show-menu-btn="!hideMenu"
      />
    </div>

    <div class="main-container">
      <div class="menu-container">
        <MenuComponent
          :visible="!hideMenu && menuVisible"
          @item-click="menuVisible = false"
        />
      </div>
      <div
        class="content"
        :class="{ 'menu-open': menuVisible && !hideMenu }"
      >
        <router-view />
      </div>
    </div>
    <GlobalPopups />
  </div>
</template>

<script>
import HeaderComponent from './components/HeaderComponent.vue'
import MenuComponent from './components/MenuComponent.vue'
import GlobalPopups from './components/GlobalPopups.vue'

export default {
  name: 'App',
  components: { HeaderComponent, MenuComponent, GlobalPopups },
  data() {
    return {
      menuVisible: window.innerWidth > 768
    }
  },
  computed: {
    hideMenu() {
      return ['/login', '/signup', '/404', '/signup-reason', '/github-callback', '/twitter-callback', '/discord-callback', '/forgot-password'].includes(this.$route.path)
    }
  },
  async mounted() {
    // placeholder for future global initializations
  },
  methods: {}
}
</script>

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
  flex: 1;
  max-width: 100%;
  transition: max-width 0.3s ease;
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
