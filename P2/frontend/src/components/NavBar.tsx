import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api'

export default function NavBar() {
  const { username, role, setLoggedOut } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      /* ignore */
    } finally {
      setLoggedOut()
      navigate('/login')
    }
  }

  return (
    <nav className="bg-gray-900 border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-6 h-14 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to="/courses" className="text-white font-bold tracking-tight">
            Dev<span className="text-blue-500">Learn</span>
          </Link>
          <Link to="/courses" className="text-sm text-gray-300 hover:text-white">
            강의 둘러보기
          </Link>
          <Link to="/my/courses" className="text-sm text-gray-300 hover:text-white">
            내 강의실
          </Link>
          <Link to="/cart" className="text-sm text-gray-300 hover:text-white">
            장바구니
          </Link>
          <Link to="/orders" className="text-sm text-gray-300 hover:text-white">
            주문 내역
          </Link>
          {role === 'INSTRUCTOR' && (
            <Link
              to="/instructor/dashboard"
              className="text-sm font-medium text-blue-400 hover:text-blue-300"
            >
              강사 콘솔
            </Link>
          )}
        </div>
        <div className="flex items-center gap-3 text-sm">
          {username ? (
            <>
              <span className="text-gray-400">
                {username}
                {role === 'INSTRUCTOR' && <span className="ml-1 text-blue-400">(강사)</span>}
              </span>
              <button
                onClick={handleLogout}
                className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded-md transition"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link to="/login" className="text-blue-400 hover:text-blue-300">
              로그인
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}
