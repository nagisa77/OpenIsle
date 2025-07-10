<template>
  <Dropdown
    v-model="selected"
    :fetch-options="fetchTags"
    multiple
    placeholder="选择标签"
    remote
  />
</template>

<script>
import { computed, ref } from 'vue'
import { API_BASE_URL, toast } from '../main'
import Dropdown from './Dropdown.vue'

export default {
  name: 'TagSelect',
  components: { Dropdown },
  props: {
    modelValue: { type: Array, default: () => [] },
    creatable: { type: Boolean, default: false }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const tags = ref([])

    const loadTags = async () => {
      if (tags.value.length) return
      const res = await fetch(`${API_BASE_URL}/api/tags`)
      if (!res.ok) return
      const data = await res.json()
      tags.value = [{ id: 0, name: '无标签' }, ...data]
    }

    const fetchTags = async (kw = '') => {
      await loadTags()
      let options = tags.value.filter(t =>
        !kw || t.name.toLowerCase().includes(kw.toLowerCase())
      )
      if (
        props.creatable &&
        kw &&
        !tags.value.some(t => t.name.toLowerCase() === kw.toLowerCase())
      ) {
        options = [
          ...options,
          { id: `__create__:${kw}`, name: `创建"${kw}"` }
        ]
      }
      return options
    }

    const selected = computed({
      get: () => props.modelValue,
      set: v => {
        if (Array.isArray(v)) {
          if (v.includes(0)) {
            emit('update:modelValue', [])
            return
          }
          if (v.length > 2) {
            toast.error('最多选择两个标签')
            return
          }
          v = v.map(id => {
            if (typeof id === 'string' && id.startsWith('__create__:')) {
              const name = id.slice(11)
              const newId = `__new__:${name}`
              if (!tags.value.find(t => t.id === newId)) {
                tags.value.push({ id: newId, name })
              }
              return newId
            }
            return id
          })
        }
        emit('update:modelValue', v)
      }
    })

    return { fetchTags, selected }
  }
}
</script>
