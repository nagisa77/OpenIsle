<template>
  <Dropdown
    v-model="selected"
    :fetch-options="fetchTypes"
    placeholder="选择帖子可见范围"
  />
</template>

<script>
import { computed, ref, watch } from 'vue'
import Dropdown from '~/components/Dropdown.vue'

export default {
  name: 'PostVisibleScopeSelect',
  components: { Dropdown },
  props: {
    modelValue: { type: String, default: 'ALL' },
    // options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {

    const fetchTypes = async () => {
      return [
        { id: 'ALL', name: '全部可见', icon: 'communication' },
        { id: 'ONLY_ME', name: '仅自己可见', icon: 'user-icon' },
        { id: 'ONLY_REGISTER', name: '仅注册用户可见', icon: 'peoples-two' },
      ]
    }

    const selected = computed({
      get: () => props.modelValue,
      set: (v) => emit('update:modelValue', v),
    })

    return { fetchTypes, selected }
  },
}
</script>

<style scoped></style>
