import { clearToken } from '~/utils/auth'

export default defineNuxtPlugin(() => {
  if (process.client) {
    const originalFetch = window.fetch
    window.fetch = async (input, init) => {
      const response = await originalFetch(input, init)
      if (response.status === 401) {
        try {
          const data = await response.clone().json()
          if (data && data.error === 'Invalid or expired token') {
            clearToken()
          }
        } catch (e) {
          // ignore JSON parsing errors
        }
      }
      return response
    }
  }
})
