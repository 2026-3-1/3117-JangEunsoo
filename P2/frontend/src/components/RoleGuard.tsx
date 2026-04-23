import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Role } from '../api/auth'

interface Props {
  allow: Role[]
  fallback?: string
  children: React.ReactNode
}

export default function RoleGuard({ allow, fallback = '/courses', children }: Props) {
  const { role, loading } = useAuth()
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center text-gray-400 text-sm">
        로딩 중...
      </div>
    )
  }
  if (!role) return <Navigate to="/login" replace />
  if (!allow.includes(role)) return <Navigate to={fallback} replace />
  return <>{children}</>
}
