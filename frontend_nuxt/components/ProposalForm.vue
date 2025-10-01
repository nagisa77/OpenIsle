<template>
  <div class="proposal-section">
    <div class="proposal-row">
      <span class="proposal-row-title">拟议分类名称</span>
      <BaseInput v-model="data.proposedName" placeholder="请输入分类名称" />
    </div>
    <div class="proposal-row">
      <span class="proposal-row-title">拟议分类 Slug</span>
      <BaseInput v-model="data.proposedSlug" placeholder="小写短横线分隔，如: tech-news" />
    </div>
    <div class="proposal-row">
      <span class="proposal-row-title">提案描述</span>
      <BaseInput v-model="data.proposalDescription" placeholder="简要说明提案目的与理由" />
    </div>
    <div class="proposal-row two-col">
      <div class="proposal-col">
        <span class="proposal-row-title">通过阈值(%)</span>
        <input
          class="number-input"
          type="number"
          v-model.number="data.approveThreshold"
          min="0"
          max="100"
        />
      </div>
      <div class="proposal-col">
        <span class="proposal-row-title">法定最小参与数</span>
        <input class="number-input" type="number" v-model.number="data.quorum" min="0" />
      </div>
    </div>
    <div class="proposal-row">
      <span class="proposal-row-title">投票结束时间</span>
      <client-only>
        <flat-pickr v-model="data.endTime" :config="dateConfig" class="time-picker" />
      </client-only>
    </div>
    <div class="proposal-row">
      <span class="proposal-row-title">投票选项</span>
      <div class="poll-option-item" v-for="(opt, idx) in data.options" :key="idx">
        <BaseInput v-model="data.options[idx]" placeholder="选项内容" />
        <i
          v-if="data.options.length > 2"
          class="fa-solid fa-xmark remove-option-icon"
          @click="removeOption(idx)"
        ></i>
      </div>
      <div class="add-option" @click="addOption">添加选项</div>
    </div>
  </div>
</template>

<script setup>
import 'flatpickr/dist/flatpickr.css'
import FlatPickr from 'vue-flatpickr-component'
import BaseInput from '~/components/BaseInput.vue'

const props = defineProps({
  data: { type: Object, required: true },
})

const dateConfig = { enableTime: true, time_24hr: true, dateFormat: 'Y-m-d H:i' }

const addOption = () => {
  props.data.options.push('')
}

const removeOption = (idx) => {
  if (props.data.options.length > 2) {
    props.data.options.splice(idx, 1)
  }
}
</script>

<style scoped>
.proposal-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 200px;
}
.proposal-row-title {
  font-size: 16px;
  color: var(--text-color);
  font-weight: bold;
  margin-bottom: 10px;
}
.proposal-row {
  display: flex;
  flex-direction: column;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.proposal-col {
  display: flex;
  flex-direction: column;
}
.number-input {
  max-width: 120px;
  height: 30px;
  border-radius: 5px;
  border: 1px solid var(--border-color);
  padding: 0 10px;
  font-size: 16px;
  color: var(--text-color);
  background-color: var(--lottery-background-color);
}
.poll-option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.remove-option-icon {
  cursor: pointer;
}
.add-option {
  color: var(--primary-color);
  cursor: pointer;
  width: fit-content;
  margin-top: 5px;
}
.time-picker {
  max-width: 200px;
  height: 30px;
  background-color: var(--lottery-background-color);
  border-radius: 5px;
  border: 1px solid var(--border-color);
}
</style>
