<template>
  <Dropdown v-model="selected" :fetch-options="fetchCategories" placeholder="选择分类" :initial-options="providedOptions">
    <template #option="{ option }">
      <div class="option-container">
        <div class="option-main">
          <template v-if="option.icon">
            <img v-if="isImageIcon(option.icon)" :src="option.icon" class="option-icon" :alt="option.name" />
            <i v-else :class="['option-icon', option.icon]"></i>
          </template>
          <span>{{ option.name }}</span>
          <span class="option-count" v-if="option.count > 0"> x {{ option.count }}</span>
        </div>
        <div v-if="option.description" class="option-desc">{{ option.description }}</div>
      </div>
    </template>
  </Dropdown>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { API_BASE_URL } from '../main'
import Dropdown from './Dropdown.vue'

export default {
  name: 'CategorySelect',
  components: { Dropdown },
  props: {
    modelValue: { type: [String, Number], default: '' },
    options: { type: Array, default: () => [] }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const providedOptions = ref(Array.isArray(props.options) ? [...props.options] : [])

    watch(
      () => props.options,
      val => {
        providedOptions.value = Array.isArray(val) ? [...val] : []
      }
    )

    const fetchCategories = async () => {
      const res = await fetch(`${API_BASE_URL}/api/categories`)
      if (!res.ok) return []
      const data = await res.json()
      return [{ id: '', name: '无分类' }, ...data]
    }

    const isImageIcon = icon => {
      if (!icon) return false
      return /^https?:\/\//.test(icon) || icon.startsWith('/')
    }

    const selected = computed({
      get: () => props.modelValue,
      set: v => emit('update:modelValue', v)
    })

    return { fetchCategories, selected, isImageIcon, providedOptions }
  }
}
</script>

<style scoped>
.option-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.option-main {
  display: flex;
  align-items: center;
  gap: 5px;
}

.option-desc {
  font-size: 12px;
  color: #666;
}

.option-count {
  font-weight: bold;
  opacity: 0.4;
}
</style>
