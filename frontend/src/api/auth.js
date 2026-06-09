const BASE = '/auth'

export async function login(email, password) {
  const res = await fetch(`${BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || `HTTP ${res.status}`)
  return res.json()
}

export async function register(name, email, password, preferredLanguage) {
  const res = await fetch(`${BASE}/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password, preferredLanguage }),
  })
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || `HTTP ${res.status}`)
  return res.json()
}

export async function googleLogin(idToken) {
  const res = await fetch(`${BASE}/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  })
  if (res.status === 409) throw new Error('This email is already registered with a password. Please log in with your password.')
  if (!res.ok) throw new Error(`Google login failed (HTTP ${res.status})`)
  return res.json()
}
