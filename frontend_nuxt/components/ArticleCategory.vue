<template>
  <div class="article-category-container" v-if="category">
    <div class="article-info-item" @click="gotoCategory">
      <BaseImage
        v-if="isImageIcon(category.smallIcon)"
        class="article-info-item-img"
        :src="category.smallIcon"
        :alt="category.name"
      />
      <component v-else :is="category.smallIcon || category.icon" class="article-info-item-img" />
      <div class="article-info-item-text">{{ category.name }}</div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  category: { type: Object, default: null },
})

const gotoCategory = async () => {
  if (!props.category) return
  const value = encodeURIComponent(props.category.id ?? props.category.name)
  await navigateTo({ path: '/', query: { category: value } })
}

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}
</script>

<style scoped>
.article-category-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  min-height: 25px;
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

  .article-category-container {
    min-height: 20px;
  }
}
</style>
