import { createContext, useContext } from 'react'
import type { Role } from '../api/auth'

export interface AuthState {
  userId: number | null
  username: string | null
  role: Role | null
  loading: boolean
  refresh: () => Promise<void>
  setLoggedOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
