// 导出一个便捷的 toast 对象
export const toast = {
  success: async (message) => {
    if (process.client) {
      try {
        const { useToast } = await import('vue-toastification')
        const toastInstance = useToast()
        toastInstance.success(message)
      } catch (error) {
        console.warn('Toast not available:', error)
      }
    }
  },
  error: async (message) => {
    if (process.client) {
      try {
        const { useToast } = await import('vue-toastification')
        const toastInstance = useToast()
        toastInstance.error(message)
      } catch (error) {
        console.warn('Toast not available:', error)
      }
    }
  },
  warning: async (message) => {
    if (process.client) {
      try {
        const { useToast } = await import('vue-toastification')
        const toastInstance = useToast()
        toastInstance.warning(message)
      } catch (error) {
        console.warn('Toast not available:', error)
      }
    }
  },
  info: async (message) => {
    if (process.client) {
      try {
        const { useToast } = await import('vue-toastification')
        const toastInstance = useToast()
        toastInstance.info(message)
      } catch (error) {
        console.warn('Toast not available:', error)
      }
    }
  },
}

// 导出 useToast composable
export const useToast = () => {
  if (process.client) {
    return new Promise(async (resolve) => {
      try {
        const { useToast: useVueToast } = await import('vue-toastification')
        resolve(useVueToast())
      } catch (error) {
        console.warn('Toast not available:', error)
        resolve({
          success: () => {},
          error: () => {},
          warning: () => {},
          info: () => {},
        })
      }
    })
  }

  return Promise.resolve({
    success: () => {},
    error: () => {},
    warning: () => {},
    info: () => {},
  })
}
