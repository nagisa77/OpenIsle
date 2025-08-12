<template>
  <div class="dropdown" ref="wrapper">
    <div class="dropdown-display" @click="toggle">
      <slot
        name="display"
        :selected="selectedLabels"
        :toggle="toggle"
        :search="search"
        :setSearch="setSearch"
      >
        <template v-if="multiple">
          <span v-if="selectedLabels.length">
            <template v-for="(label, idx) in selectedLabels" :key="label.id">
              <div class="selected-label">
                <template v-if="label.icon">
                  <img
                    v-if="isImageIcon(label.icon)"
                    :src="label.icon"
                    class="option-icon"
                    :alt="label.name"
                  />
                  <i v-else :class="['option-icon', label.icon]"></i>
                </template>
                <span>{{ label.name }}</span>
              </div>
              <span v-if="idx !== selectedLabels.length - 1">, </span>
            </template>
          </span>
          <span v-else class="placeholder">{{ placeholder }}</span>
        </template>
        <template v-else>
          <span v-if="selectedLabels.length">
            <div class="selected-label">
              <template v-if="selectedLabels[0].icon">
                <img
                  v-if="isImageIcon(selectedLabels[0].icon)"
                  :src="selectedLabels[0].icon"
                  class="option-icon"
                  :alt="selectedLabels[0].name"
                />
                <i v-else :class="['option-icon', selectedLabels[0].icon]"></i>
              </template>
              <span>{{ selectedLabels[0].name }}</span>
            </div>
          </span>
          <span v-else class="placeholder">{{ placeholder }}</span>
        </template>
        <i class="fas fa-caret-down dropdown-caret"></i>
      </slot>
    </div>
    <div
      v-if="open && !isMobile && (loading || filteredOptions.length > 0 || showSearch)"
      :class="['dropdown-menu', menuClass]"
      v-click-outside="close"
    >
      <div v-if="showSearch" class="dropdown-search">
        <i class="fas fa-search search-icon"></i>
        <input type="text" v-model="search" placeholder="搜索" />
      </div>
      <div v-if="loading" class="dropdown-loading">
        <l-hatch size="20" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <template v-else>
        <div
          v-for="o in filteredOptions"
          :key="o.id"
          @click="select(o.id)"
          :class="['dropdown-option', optionClass, { selected: isSelected(o.id) }]"
        >
          <slot name="option" :option="o" :isSelected="isSelected(o.id)">
            <template v-if="o.icon">
              <img v-if="isImageIcon(o.icon)" :src="o.icon" class="option-icon" :alt="o.name" />
              <i v-else :class="['option-icon', o.icon]"></i>
            </template>
            <span>{{ o.name }}</span>
          </slot>
        </div>
      </template>
    </div>
    <Teleport to="body">
      <div v-if="open && isMobile" class="dropdown-mobile-page">
        <div class="dropdown-mobile-header">
          <i class="fas fa-arrow-left" @click="close"></i>
          <span class="mobile-title">{{ placeholder }}</span>
        </div>
        <div class="dropdown-mobile-menu">
          <div v-if="showSearch" class="dropdown-search">
            <i class="fas fa-search search-icon"></i>
            <input type="text" v-model="search" placeholder="搜索" />
          </div>
          <div v-if="loading" class="dropdown-loading">
            <l-hatch size="20" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>
          <template v-else>
            <div
              v-for="o in filteredOptions"
              :key="o.id"
              @click="select(o.id)"
              :class="['dropdown-option', optionClass, { selected: isSelected(o.id) }]"
            >
              <slot name="option" :option="o" :isSelected="isSelected(o.id)">
                <template v-if="o.icon">
                  <img v-if="isImageIcon(o.icon)" :src="o.icon" class="option-icon" :alt="o.name" />
                  <i v-else :class="['option-icon', o.icon]"></i>
                </template>
                <span>{{ o.name }}</span>
              </slot>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted } from 'vue'
import { useIsMobile } from '~/utils/screen'

export default {
  name: 'BaseDropdown',
  props: {
    modelValue: { type: [Array, String, Number], default: () => [] },
    placeholder: { type: String, default: '返回' },
    multiple: { type: Boolean, default: false },
    fetchOptions: { type: Function, required: true },
    remote: { type: Boolean, default: false },
    menuClass: { type: String, default: '' },
    optionClass: { type: String, default: '' },
    showSearch: { type: Boolean, default: true },
    initialOptions: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue', 'update:search', 'close'],
  setup(props, { emit, expose }) {
    const open = ref(false)
    const search = ref('')
    const setSearch = (val) => {
      search.value = val
    }
    const options = ref(Array.isArray(props.initialOptions) ? [...props.initialOptions] : [])
    const loaded = ref(false)
    const loading = ref(false)
    const wrapper = ref(null)
    const isMobile = useIsMobile()

    const toggle = () => {
      open.value = !open.value
      if (!open.value) emit('close')
    }

    const close = () => {
      open.value = false
      emit('close')
    }

    const select = (id) => {
      if (props.multiple) {
        const arr = Array.isArray(props.modelValue) ? [...props.modelValue] : []
        const idx = arr.indexOf(id)
        if (idx > -1) {
          arr.splice(idx, 1)
        } else {
          arr.push(id)
        }
        emit('update:modelValue', arr)
      } else {
        emit('update:modelValue', id)
        close()
      }
      search.value = ''
    }

    const filteredOptions = computed(() => {
      if (props.remote) return options.value
      if (!search.value) return options.value
      return options.value.filter((o) => o.name.toLowerCase().includes(search.value.toLowerCase()))
    })

    const loadOptions = async (kw = '') => {
      if (!props.remote && loaded.value) return
      try {
        loading.value = true
        const res = await props.fetchOptions(props.remote ? kw : undefined)
        options.value = Array.isArray(res) ? res : []
        if (!props.remote) loaded.value = true
      } catch {
        options.value = []
      } finally {
        loading.value = false
      }
    }

    watch(
      () => props.initialOptions,
      (val) => {
        if (Array.isArray(val)) {
          options.value = [...val]
        }
      },
    )

    watch(open, async (val) => {
      if (val) {
        if (props.remote) {
          await loadOptions(search.value)
        } else if (!loaded.value) {
          await loadOptions()
        }
      }
    })

    watch(search, async (val) => {
      emit('update:search', val)
      if (props.remote && open.value) {
        await loadOptions(val)
      }
    })

    onMounted(async () => {
      if (!props.remote) {
        loadOptions()
      }
    })

    const selectedLabels = computed(() => {
      if (props.multiple) {
        return options.value.filter((o) => (props.modelValue || []).includes(o.id))
      }
      const match = options.value.find((o) => o.id === props.modelValue)
      return match ? [match] : []
    })

    const isSelected = (id) => {
      return selectedLabels.value.some((label) => label.id === id)
    }

    const isImageIcon = (icon) => {
      if (!icon) return false
      return /^https?:\/\//.test(icon) || icon.startsWith('/')
    }

    expose({ toggle, close })

    return {
      open,
      toggle,
      close,
      select,
      search,
      filteredOptions,
      wrapper,
      selectedLabels,
      isSelected,
      loading,
      isImageIcon,
      setSearch,
      isMobile,
    }
  },
}
</script>

<style>
.dropdown {
  position: relative;
}

.dropdown-display {
  border: 1px solid var(--normal-border-color);
  border-radius: 5px;
  padding: 5px 10px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 100px;
}

.placeholder {
  color: gray;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  z-index: 10000;
  max-height: 200px;
  min-width: 350px;
  overflow-y: auto;
}

.selected-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-right: 5px;
}

.dropdown-search {
  display: flex;
  align-items: center;
  padding: 5px;
  border-bottom: 1px solid var(--normal-border-color);
}

.dropdown-search input {
  flex: 1;
  border: none;
  outline: none;
  margin-left: 5px;
  background-color: var(--menu-background-color);
  color: var(--text-color);
}

.dropdown-option {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  gap: 5px;
  cursor: pointer;
}

.dropdown-option.selected {
  background-color: var(--menu-selected-background-color);
}

.dropdown-option:hover {
  background-color: var(--menu-selected-background-color);
}

.option-icon {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dropdown-loading {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.dropdown-mobile-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--menu-background-color);
  z-index: 1300;
  display: flex;
  flex-direction: column;
}

.dropdown-mobile-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-bottom: 1px solid var(--normal-border-color);
}

.dropdown-mobile-menu {
  flex: 1;
  overflow-y: auto;
}
</style>
