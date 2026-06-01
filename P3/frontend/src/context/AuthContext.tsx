import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { fetchMe, type Role } from '../api/auth'

interface AuthState {
  userId: number | null
  username: string | null
  role: Role | null
  loading: boolean
  refresh: () => Promise<void>
  setLoggedOut: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [userId, setUserId] = useState<number | null>(null)
  const [username, setUsername] = useState<string | null>(null)
  const [role, setRole] = useState<Role | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setUserId(null)
      setUsername(null)
      setRole(null)
      setLoading(false)
      return
    }
    try {
      const me = await fetchMe()
      setUserId(me.userId)
      setUsername(me.username)
      setRole(me.role)
    } catch {
      setUserId(null)
      setUsername(null)
      setRole(null)
    } finally {
      setLoading(false)
    }
  }

  const setLoggedOut = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUserId(null)
    setUsername(null)
    setRole(null)
  }

  useEffect(() => {
    refresh()
  }, [])

  return (
    <AuthContext.Provider value={{ userId, username, role, loading, refresh, setLoggedOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
