import { API_BASE_URL, DISCORD_CLIENT_ID, toast } from '../main'
import { WEBSITE_BASE_URL } from '../constants'
import { setToken, loadCurrentUser } from './auth'
import { registerPush } from './push'

export function discordAuthorize(state = '') {
  if (!DISCORD_CLIENT_ID) {
    toast.error('Discord 登录不可用')
    return
  }
  const redirectUri = `${WEBSITE_BASE_URL}/discord-callback`
  const url = `https://discord.com/api/oauth2/authorize?client_id=${DISCORD_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=identify%20email&state=${state}`
  window.location.href = url
}

export async function discordExchange(code, state, reason) {
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/discord`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code,
        redirectUri: `${window.location.origin}/discord-callback`,
        reason,
        state,
      }),
    })
    const data = await res.json()
    if (res.ok && data.token) {
      setToken(data.token)
      await loadCurrentUser()
      toast.success('登录成功')
      registerPush()
      return {
        success: true,
        needReason: false,
      }
    } else if (data.reason_code === 'NOT_APPROVED') {
      toast.info('当前为注册审核模式，请填写注册理由')
      return {
        success: false,
        needReason: true,
        token: data.token,
      }
    } else if (data.reason_code === 'IS_APPROVING') {
      toast.info('您的注册理由正在审批中')
      return {
        success: true,
        needReason: false,
      }
    } else {
      toast.error(data.error || '登录失败')
      return {
        success: false,
        needReason: false,
        error: data.error || '登录失败',
      }
    }
  } catch (e) {
    toast.error('登录失败')
    return {
      success: false,
      needReason: false,
      error: '登录失败',
    }
  }
}
