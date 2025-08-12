<template>
  <div class="achievements-list">
    <div
      v-for="medal in sortedMedals"
      :key="medal.type"
      :class="[
        'achievements-list-item',
        { select: medal.selected && canSelect, clickable: canSelect },
      ]"
      @click="selectMedal(medal)"
    >
      <img
        :src="medal.icon"
        :alt="medal.title"
        :class="['achievements-list-item-icon', { not_completed: !medal.completed }]"
      />
      <div v-if="medal.selected && canSelect" class="achievements-list-item-top-right-label">
        展示
      </div>
      <div class="achievements-list-item-title">{{ medal.title }}</div>
      <div class="achievements-list-item-description">
        {{ medal.description }}
        <template v-if="medal.type === 'COMMENT'">
          {{ medal.currentCommentCount }}/{{ medal.targetCommentCount }}
        </template>
        <template v-else-if="medal.type === 'POST'">
          {{ medal.currentPostCount }}/{{ medal.targetPostCount }}
        </template>
        <template v-else-if="medal.type === 'CONTRIBUTOR'">
          {{ medal.currentContributionLines }}/{{ medal.targetContributionLines }}
        </template>
        <template v-else-if="medal.type === 'PIONEER'"> 第{{ medal.rank }}位 </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { API_BASE_URL, toast } from '~/main'
import { getToken } from '~/utils/auth'

const props = defineProps({
  medals: {
    type: Array,
    required: true,
    default: () => [],
  },
  canSelect: {
    type: Boolean,
    default: false,
  },
})

const sortedMedals = computed(() => {
  return [...props.medals].sort((a, b) => {
    if (a.completed === b.completed) return 0
    return a.completed ? -1 : 1
  })
})

const selectMedal = async (medal) => {
  if (!props.canSelect || medal.selected) return
  if (!medal.completed) {
    toast('该勋章尚未完成')
    return
  }
  try {
    const res = await fetch(`${API_BASE_URL}/api/medals/select`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`,
      },
      body: JSON.stringify({ type: medal.type }),
    })
    if (res.ok) {
      props.medals.forEach((m) => {
        m.selected = m.type === medal.type
      })
      toast('展示勋章已更新')
    } else {
      toast('选择勋章失败')
    }
  } catch (e) {
    toast('选择勋章失败')
  }
}
</script>

<style scoped>
.achievements-list {
  padding: 20px;
  display: flex;
  flex-direction: flex-start;
  flex-wrap: wrap;
  gap: 10px;
}

.achievements-list-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
  padding: 10px;
  border-radius: 10px;
}

.achievements-list-item.clickable {
  cursor: pointer;
}

.achievements-list-item.select {
  border: 2px solid var(--primary-color);
}

.achievements-list-item-icon {
  width: 200px;
  height: 200px;
}

.achievements-list-item-title {
  font-size: 16px;
  font-weight: bold;
}

.achievements-list-item-description {
  font-size: 14px;
  color: #666;
  text-align: center;
}

.not_completed {
  filter: grayscale(100%);
}

.achievements-list-item-top-right-label {
  font-size: 10px;
  color: white;
  background-color: var(--primary-color);
  padding: 2px 4px;
  border-radius: 2px;
  position: absolute;
  top: 10px;
  right: 10px;
}

@media (max-width: 768px) {
  .achievements-list-item-icon {
    width: 100px;
    height: 100px;
  }

  .achievements-list-item-title {
    font-size: 14px;
  }

  .achievements-list-item-description {
    font-size: 12px;
  }

  .achievements-list-item {
    width: calc(50% - 27px);
    align-items: center;
    justify-content: center;
    gap: 10px;
  }
}
</style>
