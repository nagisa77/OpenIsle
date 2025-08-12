<template>
  <div class="base-input">
    <i v-if="icon" :class="['base-input-icon', icon]" />

    <!-- 普通输入框 -->
    <input
      v-if="!textarea"
      class="base-input-text"
      :type="type"
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />

    <!-- 多行输入框 -->
    <textarea
      v-else
      class="base-input-text"
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
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
    textarea: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  computed: {
    innerValue: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      },
    },
  },
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
  border: 1px solid var(--normal-border-color);
  gap: 10px;
}

.base-input:focus-within {
  border-color: var(--primary-color);
}

.base-input-icon {
  opacity: 0.5;
  font-size: 14px;
}

.base-input-text {
  border: none;
  outline: none;
  width: 100%;
  font-size: 14px;
  resize: none;
  background-color: transparent;
  color: var(--text-color);
}
</style>
