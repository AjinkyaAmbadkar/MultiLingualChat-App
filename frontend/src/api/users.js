const BASE = '/api/users'

export async function getMe(token) {
  const res = await fetch(`${BASE}/me`, { headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) throw new Error('Failed to fetch profile')
  return res.json()
}

export async function getAllUsers(token) {
  const res = await fetch(BASE, { headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) throw new Error('Failed to fetch users')
  return res.json()
}

export async function fetchOnlineStatus(token, userId) {
  const res = await fetch(`${BASE}/${userId}/online`, { headers: { Authorization: `Bearer ${token}` } })
  if (!res.ok) return false
  const data = await res.json()
  return data.online
}

export async function updateLanguage(token, preferredLanguage) {
  const res = await fetch(`${BASE}/me/language`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ preferredLanguage }),
  })
  if (!res.ok) throw new Error('Failed to update language')
  return res.json()
}
