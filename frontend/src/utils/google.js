import { API_BASE_URL, GOOGLE_CLIENT_ID, toast } from '../main'
import { setToken, loadCurrentUser } from './auth'
import { registerPush } from './push'

export async function googleGetIdToken() {
  return new Promise((resolve, reject) => {
    if (!window.google || !GOOGLE_CLIENT_ID) {
      toast.error('Google 登录不可用, 请检查网络设置与VPN')
      reject()
      return
    }
    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: ({ credential }) => resolve(credential),
      use_fedcm: true
    })
    window.google.accounts.id.prompt()
  })
}

export async function googleGetIdTokenWithPop() {
  return new Promise((resolve, reject) => {
    if (!window.google || !GOOGLE_CLIENT_ID) {
      toast.error('Google 登录不可用, 请检查网络设置与VPN')
      reject()
      return
    }
    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: ({ credential }) => resolve(credential),
      ux_mode: 'popup'
    })
    window.google.accounts.id.prompt()
  })
}

async function googleGetIdTokenWithNewWindow() {
  return new Promise((resolve, reject) => {
    if (!GOOGLE_CLIENT_ID) {
      toast.error('Google 登录不可用, 请检查网络设置与VPN')
      reject()
      return
    }
    const width = 500
    const height = 600
    const left = (window.screen.width - width) / 2
    const top = (window.screen.height - height) / 2
    const popup = window.open('', 'google-login', `width=${width},height=${height},left=${left},top=${top}`)
    if (!popup) {
      toast.error('弹出窗口被拦截')
      reject()
      return
    }
    popup.document.write('<!doctype html><html><head><title>Google 登录</title></head><body></body></html>')
    const script = popup.document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.onload = () => {
      popup.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: ({ credential }) => {
          popup.close()
          resolve(credential)
        },
        ux_mode: 'popup'
      })
      popup.google.accounts.id.prompt()
    }
    script.onerror = () => {
      popup.close()
      reject()
    }
    popup.document.head.appendChild(script)
  })
}

export async function googleAuthWithToken(idToken, redirect_success, redirect_not_approved) {
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken })
    })
    const data = await res.json()
    if (res.ok && data.token) {
      setToken(data.token)
      await loadCurrentUser()
      toast.success('登录成功')
      registerPush()
      if (redirect_success) redirect_success()
    } else if (data.reason_code === 'NOT_APPROVED') {
      toast.info('当前为注册审核模式，请填写注册理由')
      if (redirect_not_approved) redirect_not_approved(data.token)
    } else if (data.reason_code === 'IS_APPROVING') {
      toast.info('您的注册理由正在审批中')      
      if (redirect_success) redirect_success()
    }
  } catch (e) {
    toast.error('登录失败')
  }
}

export async function googleSignIn(redirect_success, redirect_not_approved) {
  try {
    const token = await googleGetIdToken()
    await googleAuthWithToken(token, redirect_success, redirect_not_approved)
  } catch {
    /* ignore */
  }
}

export async function googleSignInWithPop(redirect_success, redirect_not_approved) {
  try {
    const token = await googleGetIdTokenWithPop()
    await googleAuthWithToken(token, redirect_success, redirect_not_approved)
  } catch {
    /* ignore */
  }
}

async function googleSignInWithNewWindow(redirect_success, redirect_not_approved) {
  try {
    const token = await googleGetIdTokenWithNewWindow()
    await googleAuthWithToken(token, redirect_success, redirect_not_approved)
  } catch {
    /* ignore */
  }
}

import router from '../router'

export function loginWithGoogle() {
  googleSignIn(
    () => {
      router.push('/')
    },
    token => {
      router.push('/signup-reason?token=' + token)
    }
  )
}

export function loginWithGoogleWithPop() {
  googleSignInWithPop(
    () => {
      router.push('/')
    },
    token => {
      router.push('/signup-reason?token=' + token)
    }
  )
}

export function loginWithGoogleWithNewWindow() {
  googleSignInWithNewWindow(
    () => {
      router.push('/')
    },
    token => {
      router.push('/signup-reason?token=' + token)
    }
  )
}