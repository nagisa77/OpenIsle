import { API_BASE_URL, TWITTER_CLIENT_ID, toast } from '../main'
import { WEBSITE_BASE_URL } from '../constants'
import { setToken, loadCurrentUser } from './auth'
import { registerPush } from './push'

function generateCodeVerifier() {
  const array = new Uint8Array(32)
  window.crypto.getRandomValues(array)
  return Array.from(array)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

async function generateCodeChallenge(codeVerifier) {
  const encoder = new TextEncoder()
  const data = encoder.encode(codeVerifier)
  const digest = await window.crypto.subtle.digest('SHA-256', data)
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

export async function twitterAuthorize(state = '') {
  if (!TWITTER_CLIENT_ID) {
    toast.error('Twitter 登录不可用')
    return
  }
  if (state === '') {
    state = Math.random().toString(36).substring(2, 15)
  }
  const redirectUri = `${WEBSITE_BASE_URL}/twitter-callback`
  const codeVerifier = generateCodeVerifier()
  sessionStorage.setItem('twitter_code_verifier', codeVerifier)
  const codeChallenge = await generateCodeChallenge(codeVerifier)
  const url =
    `https://x.com/i/oauth2/authorize?response_type=code&client_id=${TWITTER_CLIENT_ID}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}&scope=tweet.read%20users.read` +
    `&state=${state}&code_challenge=${codeChallenge}&code_challenge_method=S256`
  window.location.href = url
}

export async function twitterExchange(code, state, reason) {
  try {
    const codeVerifier = sessionStorage.getItem('twitter_code_verifier')
    sessionStorage.removeItem('twitter_code_verifier')
    const res = await fetch(`${API_BASE_URL}/api/auth/twitter`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code,
        redirectUri: `${window.location.origin}/twitter-callback`,
        reason,
        state,
        codeVerifier,
      }),
    })
    const data = await res.json()
    if (res.ok && data.token) {
      setToken(data.token)
      await loadCurrentUser()
      toast.success('登录成功')
      registerPush()
      return { success: true, needReason: false }
    } else if (data.reason_code === 'NOT_APPROVED') {
      toast.info('当前为注册审核模式，请填写注册理由')
      return { success: false, needReason: true, token: data.token }
    } else if (data.reason_code === 'IS_APPROVING') {
      toast.info('您的注册理由正在审批中')
      return { success: true, needReason: false }
    } else {
      toast.error(data.error || '登录失败')
      return { success: false, needReason: false, error: data.error || '登录失败' }
    }
  } catch (e) {
    toast.error('登录失败')
    return { success: false, needReason: false, error: '登录失败' }
  }
}
