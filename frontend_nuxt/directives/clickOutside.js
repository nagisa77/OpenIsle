/**
 * @file clickOutsideDirective.js
 * @description 一个用于检测元素外部点击的Vue 3自定义指令。
 *
 * @example
 * // 在 main.js 中全局注册
 * import { createApp } from 'vue'
 * import App from './App.vue'
 * import ClickOutside from './clickOutsideDirective.js'
 *
 * const app = createApp(App)
 * app.directive('click-outside', ClickOutside)
 * app.mount('#app')
 *
 * // 在组件中使用
 * <div v-click-outside="myMethod">...</div>
 *
 * // 排除特定元素
 * <div v-click-outside:[myExcludedElement]="myMethod">...</div>
 * <div v-click-outside:[[el1, el2]]="myMethod">...</div>
 */

// 使用一个Map来存储所有指令绑定的元素及其对应的处理器
// 键是HTMLElement，值是一个包含处理器和回调函数的对象数组
const nodeList = new Map()

// 检查是否在客户端环境，以避免在SSR（服务器端渲染）时执行
const isClient = typeof window !== 'undefined'

// 在客户端环境中，只设置一次全局的 mousedown / mouseup 和 touchstart / touchend 监听器
if (isClient) {
  let startClick

  const handleStart = (e) => (startClick = e)
  const handleEnd = (e) => {
    // 遍历所有注册的元素和它们的处理器
    for (const handlers of nodeList.values()) {
      for (const { documentHandler } of handlers) {
        // 调用每个处理器，传入结束和开始事件
        documentHandler(e, startClick)
      }
    }
    // 完成后重置 startClick
    startClick = undefined
  }

  document.addEventListener('mousedown', handleStart)
  document.addEventListener('touchstart', handleStart)

  document.addEventListener('mouseup', handleEnd)
  document.addEventListener('touchend', handleEnd)
}

/**
 * 创建一个文档事件处理器。
 * @param {HTMLElement} el - 指令绑定的元素。
 * @param {import('vue').DirectiveBinding} binding - 指令的绑定对象。
 * @returns {Function} 返回一个处理函数。
 */
function createDocumentHandler(el, binding) {
  let excludes = []
  // binding.arg 可以是一个元素或一个元素数组，用于排除不需要触发回调的点击
  if (Array.isArray(binding.arg)) {
    excludes = binding.arg
  } else if (binding.arg instanceof HTMLElement) {
    excludes.push(binding.arg)
  }

  return function (mouseup, mousedown) {
    // 从组件实例中获取 popper 引用（如果存在），这对于处理下拉菜单、弹窗等很有用
    const popperRef = binding.instance?.popperRef
    const mouseUpTarget = mouseup.target
    const mouseDownTarget = mousedown?.target

    // 检查各种条件，如果满足任一条件，则不执行回调
    const isBound = !binding || !binding.instance
    const isTargetExists = !mouseUpTarget || !mouseDownTarget
    const isContainedByEl = el.contains(mouseUpTarget) || el.contains(mouseDownTarget)
    const isSelf = el === mouseUpTarget

    // 检查点击是否发生在任何被排除的元素内部
    const isTargetExcluded =
      (excludes.length && excludes.some((item) => item?.contains(mouseUpTarget))) ||
      (excludes.length && excludes.includes(mouseDownTarget))

    // 检查点击是否发生在关联的 popper 元素内部
    const isContainedByPopper =
      popperRef && (popperRef.contains(mouseUpTarget) || popperRef.contains(mouseDownTarget))

    if (
      isBound ||
      isTargetExists ||
      isContainedByEl ||
      isSelf ||
      isTargetExcluded ||
      isContainedByPopper
    ) {
      return
    }

    // 如果所有检查都通过，说明点击发生在外部，执行指令传入的回调函数
    binding.value(mouseup, mousedown)
  }
}

const ClickOutside = {
  /**
   * 在绑定元素的 attribute 或事件监听器被应用之前调用。
   * @param {HTMLElement} el
   * @param {import('vue').DirectiveBinding} binding
   */
  beforeMount(el, binding) {
    if (!nodeList.has(el)) {
      nodeList.set(el, [])
    }

    nodeList.get(el).push({
      documentHandler: createDocumentHandler(el, binding),
      bindingFn: binding.value,
    })
  },

  /**
   * 在包含组件的 VNode 及其子组件的 VNode 更新后调用。
   * @param {HTMLElement} el
   * @param {import('vue').DirectiveBinding} binding
   */
  updated(el, binding) {
    if (!nodeList.has(el)) {
      nodeList.set(el, [])
    }

    const handlers = nodeList.get(el)
    // 查找旧的回调函数对应的处理器
    const oldHandlerIndex = handlers.findIndex((item) => item.bindingFn === binding.oldValue)

    const newHandler = {
      documentHandler: createDocumentHandler(el, binding),
      bindingFn: binding.value,
    }

    if (oldHandlerIndex >= 0) {
      // 如果找到了，就替换成新的处理器
      handlers.splice(oldHandlerIndex, 1, newHandler)
    } else {
      // 否则，直接添加新的处理器
      handlers.push(newHandler)
    }
  },

  /**
   * 在绑定元素的父组件卸载后调用。
   * @param {HTMLElement} el
   */
  unmounted(el) {
    // 当元素卸载时，从Map中移除它，以进行垃圾回收并防止内存泄漏
    nodeList.delete(el)
  },
}

export default ClickOutside
