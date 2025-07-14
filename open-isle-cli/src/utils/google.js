import { API_BASE_URL, GOOGLE_CLIENT_ID, toast } from '../main'
import { setToken, loadCurrentUser } from './auth'

export function googleSignIn(redirect, reason) {
  if (!window.google || !GOOGLE_CLIENT_ID) {
    toast.error('Google 登录不可用')
    return
  }
  window.google.accounts.id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: async ({ credential }) => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/google`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ idToken: credential, reason })
        })
        const data = await res.json()
        if (res.ok && data.token) {
          setToken(data.token)
          await loadCurrentUser()
          toast.success('登录成功')
          if (redirect) redirect()
        } else {
          toast.error(data.error || '登录失败')
        }
      } catch (e) {
        toast.error('登录失败')
      }
    }
  })
  window.google.accounts.id.prompt()
}
