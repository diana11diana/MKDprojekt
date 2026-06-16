import { useEffect, useMemo, useState } from 'react'
import {
  loginUser,
  logoutUser,
  refreshSession,
  registerUser,
  setAccessToken,
  updateMyProfile,
  verifyEmail,
} from './api'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [initializing, setInitializing] = useState(true)

  useEffect(() => {
    refreshSession()
      .then((session) => {
        setAccessToken(session.accessToken)
        setUser(session.user)
      })
      .catch(() => {
        setAccessToken(null)
        setUser(null)
      })
      .finally(() => setInitializing(false))
  }, [])

  const value = useMemo(() => ({
    user,
    initializing,
    async login(credentials) {
      const session = await loginUser(credentials)
      setAccessToken(session.accessToken)
      setUser(session.user)
      return session.user
    },
    async register(data) {
      const result = await registerUser(data)
      if (result.verificationToken) {
        await verifyEmail(result.verificationToken)
        const session = await loginUser({ email: data.email, password: data.password })
        setAccessToken(session.accessToken)
        setUser(session.user)
      }
      return result
    },
    async logout() {
      try {
        await logoutUser()
      } finally {
        setAccessToken(null)
        setUser(null)
      }
    },
    async updateProfile(data) {
      const updatedUser = await updateMyProfile(data)
      setUser(updatedUser)
      return updatedUser
    },
  }), [initializing, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
