import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api'
import { getCart } from '../api/cart'

export default function NavBar() {
  const { username, role, setLoggedOut } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [cartCount, setCartCount] = useState(0)

  useEffect(() => {
    if (!username || role === 'INSTRUCTOR') {
      setCartCount(0)
      return
    }
    getCart()
      .then((c) => setCartCount(c.itemCount))
      .catch(() => setCartCount(0))
  }, [username, role, location.pathname])

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
          <Link to="/my/bookmarks" className="text-sm text-gray-300 hover:text-white">
            북마크
          </Link>
          <Link to="/cart" className="text-sm text-gray-300 hover:text-white relative">
            장바구니
            {cartCount > 0 && (
              <span className="absolute -top-2 -right-4 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center">
                {cartCount}
              </span>
            )}
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
          {role === 'ADMIN' && (
            <Link
              to="/admin"
              className="text-sm font-medium text-amber-400 hover:text-amber-300"
            >
              관리자 콘솔
            </Link>
          )}
        </div>
        <div className="flex items-center gap-3 text-sm">
          {username ? (
            <>
              <span className="text-gray-400">
                {username}
                {role === 'INSTRUCTOR' && <span className="ml-1 text-blue-400">(강사)</span>}
                {role === 'ADMIN' && <span className="ml-1 text-amber-400">(관리자)</span>}
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
