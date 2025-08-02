<template>
  <div class="post-editor-container">
    <div :id="editorId" ref="vditorElement"></div>
    <div v-if="loading" class="editor-loading-overlay">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { themeState } from '../utils/theme'
import {
  createVditor,
  getEditorTheme as getEditorThemeUtil,
  getPreviewTheme as getPreviewThemeUtil
} from '../utils/vditor'
import { hatch } from 'ldrs'
hatch.register()


export default {
  name: 'PostEditor',
  emits: ['update:modelValue'],
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    editorId: {
      type: String,
      default: () => 'post-editor-' + Math.random().toString(36).slice(2)
    },
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { emit }) {
    const vditorInstance = ref(null)
    const getEditorTheme = getEditorThemeUtil
    const getPreviewTheme = getPreviewThemeUtil
    const applyTheme = () => {
      if (vditorInstance.value) {
        vditorInstance.value.setTheme(getEditorTheme(), getPreviewTheme())
      }
    }

    watch(
      () => props.loading,
      val => {
        if (val) {
          vditorInstance.value.disabled()
        } else {
          vditorInstance.value.enable()
        }
      }
    )

    watch(
      () => props.disabled,
      val => {
        if (!vditorInstance.value) return
        if (val) {
          vditorInstance.value.disabled()
        } else if (!props.loading) {
          vditorInstance.value.enable()
        }
      }
    )

    watch(
      () => props.modelValue,
      val => {
        if (vditorInstance.value && vditorInstance.value.getValue() !== val) {
          vditorInstance.value.setValue(val)
        }
      }
    )

    watch(
      () => themeState.mode,
      () => {
        applyTheme()
      }
    )

    onMounted(() => {
      vditorInstance.value = createVditor(props.editorId, {
        placeholder: '请输入正文...',
        input(value) {
          emit('update:modelValue', value)
        },
        after() {
          vditorInstance.value.setValue(props.modelValue)
          if (props.loading || props.disabled) {
            vditorInstance.value.disabled()
          }
          applyTheme()
        }
      })
      // applyTheme()
    })

    return {}
  }
}
</script>

<style scoped>
.post-editor-container {
  border: 1px solid var(--normal-border-color);
  position: relative;
}

.editor-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--menu-selected-background-color);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: all;
  z-index: 10;
}
</style>
