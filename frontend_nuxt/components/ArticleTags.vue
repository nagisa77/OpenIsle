<template>
  <div class="article-tags-container">
    <div
      class="article-info-item"
      v-for="tag in tags"
      :key="tag.id || tag.name"
      @click="gotoTag(tag)"
    >
      <img
        v-if="tag.smallIcon"
        class="article-info-item-img"
        :src="tag.smallIcon"
        :alt="tag.name"
      />
      <div class="article-info-item-text">{{ tag.name }}</div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'ArticleTags',
  props: {
    tags: { type: Array, default: () => [] },
  },
  setup() {
    const router = useRouter()
    const gotoTag = (tag) => {
      const value = encodeURIComponent(tag.id ?? tag.name)
      router.push({ path: '/', query: { tags: value } }).then(() => {
        window.location.reload()
      })
    }
    return { gotoTag }
  },
}
</script>

<style scoped>
.article-tags-container {
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
