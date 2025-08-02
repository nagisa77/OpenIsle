import { API_BASE_URL } from '../main'

export async function fetchFollowings(username) {
  if (!username) return []
  try {
    const res = await fetch(`${API_BASE_URL}/api/users/${username}/following`)
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}

export async function fetchAdmins() {
  try {
    const res = await fetch(`${API_BASE_URL}/api/users/admins`)
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}

export async function searchUsers(keyword) {
  if (!keyword) return []
  try {
    const res = await fetch(`${API_BASE_URL}/api/search/users?keyword=${encodeURIComponent(keyword)}`)
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}
