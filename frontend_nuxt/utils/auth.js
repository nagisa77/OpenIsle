import { API_BASE_URL } from '../main'
import { reactive } from 'vue'

const TOKEN_KEY = 'token'
const USER_ID_KEY = 'userId'
const USERNAME_KEY = 'username'
const ROLE_KEY = 'role'

export const authState = reactive({
  loggedIn: false,
  userId: null,
  username: null,
  role: null
})

authState.loggedIn = localStorage.getItem(TOKEN_KEY) !== null && localStorage.getItem(TOKEN_KEY) !== ''
authState.userId = localStorage.getItem(USER_ID_KEY)
authState.username = localStorage.getItem(USERNAME_KEY)
authState.role = localStorage.getItem(ROLE_KEY)

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
  authState.loggedIn = true
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  clearUserInfo()
  authState.loggedIn = false
}

export function setUserInfo({ id, username }) {
  authState.userId = id
  authState.username = username
  if (arguments[0] && arguments[0].role) {
    authState.role = arguments[0].role
    localStorage.setItem(ROLE_KEY, arguments[0].role)
  }
  if (id !== undefined && id !== null) localStorage.setItem(USER_ID_KEY, id)
  if (username) localStorage.setItem(USERNAME_KEY, username)
}

export function clearUserInfo() {
  localStorage.removeItem(USER_ID_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(ROLE_KEY)
  authState.userId = null
  authState.username = null
  authState.role = null
}

export async function fetchCurrentUser() {
  const token = getToken()
  if (!token) return null
  try {
    const res = await fetch(`${API_BASE_URL}/api/users/me`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!res.ok) return null
    return await res.json()
  } catch (e) {
    return null
  }
}

export async function loadCurrentUser() {
  const user = await fetchCurrentUser()
  if (user) {
    setUserInfo({ id: user.id, username: user.username, role: user.role })
  }
  return user
}

export function isLogin() {
  return authState.loggedIn
}

export async function checkToken() {
  const token = getToken()
  if (!token) return false
  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/check`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    authState.loggedIn = res.ok
    return res.ok
  } catch (e) {
    authState.loggedIn = false
    return false
  }
}
