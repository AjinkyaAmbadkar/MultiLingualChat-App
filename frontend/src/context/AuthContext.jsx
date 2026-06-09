import { createContext, useContext, useState } from 'react'

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

  function signIn(token, user) {
    localStorage.setItem('accessToken', token)
    localStorage.setItem('user', JSON.stringify(user))
    setAuth({ token, user })
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
  }

  return (
    <AuthContext.Provider value={{ auth, signIn, signOut, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
