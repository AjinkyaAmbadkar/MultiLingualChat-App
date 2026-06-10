import { createContext, useContext, useState } from 'react'
import { importPrivateKey } from '../utils/crypto'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    try {
      const token = localStorage.getItem('accessToken')
      const user  = localStorage.getItem('user')
      return token && user ? { token, user: JSON.parse(user) } : null
    } catch {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      return null
    }
  })

  // RSA private key stored in memory only — never written to localStorage
  const [privateKey, setPrivateKey] = useState(null)

  async function signIn(token, user, rawPrivateKeyB64) {
    localStorage.setItem('accessToken', token)
    localStorage.setItem('user', JSON.stringify(user))
    setAuth({ token, user })

    if (rawPrivateKeyB64) {
      try {
        const key = await importPrivateKey(rawPrivateKeyB64)
        setPrivateKey(key)
      } catch (err) {
        console.error('Failed to import private key:', err)
      }
    }
  }

  function updateUser(updatedFields) {
    const user = { ...auth.user, ...updatedFields }
    localStorage.setItem('user', JSON.stringify(user))
    setAuth(prev => ({ ...prev, user }))
  }

  function signOut() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
    setAuth(null)
    setPrivateKey(null)
  }

  return (
    <AuthContext.Provider value={{ auth, privateKey, signIn, signOut, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
