import { ref, computed, onUnmounted } from 'vue'
import { useRequestHeaders } from 'nuxt/app'

export const useIsMobile = () => {
  const width = ref(0)
  const isClient = ref(false)

  const isMobileUserAgent = () => {
    let userAgent = ''

    if (typeof navigator !== 'undefined') {
      userAgent = navigator.userAgent.toLowerCase()
    } else {
      const headers = useRequestHeaders(['user-agent'])
      userAgent = (headers['user-agent'] || '').toLowerCase()
    }

    const mobileKeywords = [
      'android', 'iphone', 'ipad', 'ipod', 'blackberry', 'windows phone',
      'mobile', 'tablet', 'opera mini', 'iemobile'
    ]

    return mobileKeywords.some(keyword => userAgent.includes(keyword))
  }

  if (typeof window !== 'undefined') {
    isClient.value = true
    const updateWidth = () => {
      width.value = window.innerWidth
    }
    updateWidth()
    window.addEventListener('resize', updateWidth)
    onUnmounted(() => {
      window.removeEventListener('resize', updateWidth)
    })
  }

  return computed(() => {
    if (isClient.value) {
      return width.value > 0 ? width.value <= 768 : isMobileUserAgent()
    }

    return isMobileUserAgent()
  })
}

