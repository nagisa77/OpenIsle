<template>
  <div class="timeline">
    <div class="timeline-item" v-for="(item, idx) in items" :key="idx">
      <div
        class="timeline-icon"
        :class="{ clickable: !!item.iconClick }"
        @click="item.iconClick && item.iconClick()"
      >
        <img v-if="item.src" :src="item.src" class="timeline-img" alt="timeline item" />
        <i v-else-if="item.icon" :class="item.icon"></i>
        <span v-else-if="item.emoji" class="timeline-emoji">{{ item.emoji }}</span>
      </div>
      <div class="timeline-content">
        <slot name="item" :item="item">{{ item.content }}</slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BaseTimeline',
  props: {
    items: { type: Array, default: () => [] }
  }
}
</script>

<style scoped>
.timeline {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.timeline-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  position: relative;
  margin-top: 10px;
}

.timeline-icon {
  position: sticky;
  top: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: var(--text-color);
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 10px;
  flex-shrink: 0;
}

.timeline-icon.clickable {
  cursor: pointer;
}

.timeline-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.timeline-emoji {
  font-size: 20px;
  line-height: 1;
}

.timeline-item::before {
  content: '';
  position: absolute;
  top: 32px;
  left: 15px;
  width: 2px;
  bottom: -20px;
  background: var(--text-color);
  opacity: 0.08;
}

.timeline-item:last-child::before {
  display: none;
}

.timeline-content {
  flex: 1;
  width: calc(100% - 32px);
}

@media (max-width: 768px) {

  .timeline-icon {
    margin-right: 2px;
    width: 30px;
    height: 30px;
  }
}

</style>
