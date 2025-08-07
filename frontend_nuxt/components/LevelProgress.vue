<template>
  <div class="level-progress">
    <div class="level-progress-current">当前Lv.{{ currentLevel }}</div>
    <ProgressBar :value="value" :max="max" />
    <div class="level-progress-info">
      <div class="level-progress-exp">{{ exp }} / {{ nextExp }}</div>
      <div class="level-progress-target">🎉目标 Lv.{{ currentLevel + 1 }}</div>
    </div>
  </div>
</template>

<script>
import ProgressBar from './ProgressBar.vue'
import { prevLevelExp } from '../utils/level'
export default {
  name: 'LevelProgress',
  components: { ProgressBar },
  props: {
    exp: { type: Number, default: 0 },
    currentLevel: { type: Number, default: 0 },
    nextExp: { type: Number, default: 0 }
  },
  computed: {
    max () {
      return this.nextExp - prevLevelExp(this.currentLevel)
    },
    value () {
      return this.exp - prevLevelExp(this.currentLevel)
    }
  }
}
</script>

<style scoped>
.level-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
  font-size: 14px;
}

.level-progress-current {
  font-weight: bold;
}

.level-progress-info {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.level-progress-exp,
.level-progress-target {
  font-size: 12px;
  opacity: 0.8;
}
</style>
