import { ref, computed, onUnmounted } from 'vue'
import { useRequestHeaders } from 'nuxt/app'

export const useIsMobile = () => {
  const width = ref(0)
  const isClient = ref(false)

  // Read the user-agent on the server during the first invocation so it is
  // available when the computed getter executes.
  const serverUserAgent = process.server
    ? (useRequestHeaders(['user-agent'])['user-agent'] || '').toLowerCase()
    : ''

  const isMobileUserAgent = ua => {
    ua = (ua || '').toLowerCase()
    const mobileKeywords = [
      'android',
      'iphone',
      'ipad',
      'ipod',
      'blackberry',
      'windows phone',
      'mobile',
      'tablet',
      'opera mini',
      'iemobile'
    ]
    return mobileKeywords.some(keyword => ua.includes(keyword))
  }

  if (process.client) {
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
      return width.value > 0
        ? width.value <= 768
        : isMobileUserAgent(navigator.userAgent)
    }

    return isMobileUserAgent(serverUserAgent)
  })
}

