<template>
  <div class="base-input">
    <i v-if="icon" :class="['base-input-icon', icon]"></i>
    <component
      :is="textarea ? 'textarea' : 'input'"
      class="base-input-text"
      :type="type"
      v-model="innerValue"
      v-bind="$attrs"
    />
  </div>
</template>

<script>
export default {
  name: 'BaseInput',
  inheritAttrs: false,
  props: {
    modelValue: { type: [String, Number], default: '' },
    icon: { type: String, default: '' },
    type: { type: String, default: 'text' },
    textarea: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'input'],
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
        this.$emit('input', val)
      }
    }
  }
}
</script>

<style scoped>
.base-input {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  width: calc(100% - 40px);
  padding: 15px 20px;
  border-radius: 10px;
  border: 1px solid #ccc;
  gap: 10px;
}

.base-input-icon {
  opacity: 0.5;
  font-size: 16px;
}

.base-input-text {
  border: none;
  outline: none;
  width: 100%;
  font-size: 16px;
}
</style>
