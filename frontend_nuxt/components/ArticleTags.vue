<template>
  <div class="article-tags-container">
    <div
      class="article-info-item"
      v-for="tag in tags"
      :key="tag.id || tag.name"
      @click="gotoTag(tag)"
    >
      <BaseImage
        v-if="isImageIcon(tag.smallIcon)"
        class="article-info-item-img"
        :src="tag.smallIcon"
        :alt="tag.name"
      />
      <component
        v-else-if="tag.smallIcon || tag.icon"
        :is="tag.smallIcon || tag.icon"
        class="article-info-item-img"
      />
      <tag-one v-else class="article-info-item-img" />
      <div class="article-info-item-text">{{ tag.name }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  tags: { type: Array, default: () => [] },
})

const gotoTag = async (tag) => {
  const value = encodeURIComponent(tag.id ?? tag.name)
  await navigateTo({ path: '/', query: { tags: value } })
}

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}
</script>

<style scoped>
.article-tags-container {
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

  .article-tags-container {
    min-height: 20px;
  }
}
</style>
