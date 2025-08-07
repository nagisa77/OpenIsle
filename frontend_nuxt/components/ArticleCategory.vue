<template>
  <div class="article-category-container" v-if="category">
    <div class="article-info-item" @click="gotoCategory">
      <img
        v-if="category.smallIcon"
        class="article-info-item-img"
        :src="category.smallIcon"
        :alt="category.name"
      />
      <div class="article-info-item-text">{{ category.name }}</div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'ArticleCategory',
  props: {
    category: { type: Object, default: null }
  },
  setup(props) {
    const router = useRouter()
    const gotoCategory = () => {
      if (!props.category) return
      const value = encodeURIComponent(props.category.id ?? props.category.name)
      router.push({ path: '/', query: { category: value } }).then(() => {
        window.location.reload()
      })
    }
    return { gotoCategory }
  }
}
</script>

<style scoped>
.article-category-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.article-info-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  padding: 2px 4px;
  background-color: var(--article-info-background-color);
  border-radius: 4px;
  cursor: pointer;
}

.article-info-item-img {
  width: 16px;
  height: 16px;
}

@media (max-width: 768px) {
  .article-info-item-img {
    width: 10px;
    height: 10px;
  }

  .article-info-item {
    font-size: 10px;
  }
}
</style>
