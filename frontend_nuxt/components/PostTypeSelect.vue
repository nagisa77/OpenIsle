<template>
  <Dropdown
    v-model="selected"
    :fetch-options="fetchTypes"
    placeholder="选择帖子类型"
    :initial-options="providedOptions"
  />
</template>

<script>
import { computed, ref, watch } from 'vue'
import Dropdown from '~/components/Dropdown.vue'

export default {
  name: 'PostTypeSelect',
  components: { Dropdown },
  props: {
    modelValue: { type: String, default: 'NORMAL' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const providedOptions = ref(Array.isArray(props.options) ? [...props.options] : [])

    watch(
      () => props.options,
      (val) => {
        providedOptions.value = Array.isArray(val) ? [...val] : []
      },
    )

    const fetchTypes = async () => {
      return [
        { id: 'NORMAL', name: '普通帖子', icon: 'fa-regular fa-file' },
        { id: 'LOTTERY', name: '抽奖帖子', icon: 'fa-solid fa-gift' },
      ]
    }

    const selected = computed({
      get: () => props.modelValue,
      set: (v) => emit('update:modelValue', v),
    })

    return { fetchTypes, selected, providedOptions }
  },
}
</script>

<style scoped></style>
