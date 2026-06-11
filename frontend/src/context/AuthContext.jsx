import { createContext, useContext, useState, useEffect } from 'react'
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

  // RSA private key: imported into memory, raw key kept in sessionStorage to survive page refresh
  const [privateKey, setPrivateKey] = useState(null)

  // On mount, re-import the private key from sessionStorage if present (page refresh case).
  // If auth exists but no key in sessionStorage, clear the session — user must log in again to get their key.
  useEffect(() => {
    const raw = sessionStorage.getItem('privateKey')
    if (raw) {
      importPrivateKey(raw)
        .then(setPrivateKey)
        .catch(() => {
          sessionStorage.removeItem('privateKey')
          localStorage.removeItem('accessToken')
          localStorage.removeItem('user')
          setAuth(null)
        })
    } else if (auth) {
      // Session token exists but private key is gone — can't decrypt anything, force re-login
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      setAuth(null)
    }
  }, [])

  async function signIn(token, user, rawPrivateKeyB64) {
    localStorage.setItem('accessToken', token)
    localStorage.setItem('user', JSON.stringify(user))
    setAuth({ token, user })

    if (rawPrivateKeyB64) {
      try {
        const key = await importPrivateKey(rawPrivateKeyB64)
        setPrivateKey(key)
        sessionStorage.setItem('privateKey', rawPrivateKeyB64)
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
    sessionStorage.removeItem('privateKey')
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
