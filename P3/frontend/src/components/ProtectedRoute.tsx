import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface Props {
  children: React.ReactNode
}

export default function ProtectedRoute({ children }: Props) {
  const { role, loading } = useAuth()
  const token = localStorage.getItem('accessToken')
  if (!token) return <Navigate to="/login" replace />
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center text-gray-400 text-sm">
        로딩 중...
      </div>
    )
  }
  if (!role) return <Navigate to="/login" replace />
  return <>{children}</>
}
