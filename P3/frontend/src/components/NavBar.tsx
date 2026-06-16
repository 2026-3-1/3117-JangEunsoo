import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import api from '../api'
import { getCart } from '../api/cart'

export default function NavBar() {
  const { username, role, setLoggedOut } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [cartCount, setCartCount] = useState(0)

  useEffect(() => {
    // 장바구니는 학생 메뉴에만 노출 — 강사/관리자는 조회 불필요
    if (!username || role !== 'STUDENT') {
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

  // 역할별 홈 + 브랜드 색
  const home = role === 'ADMIN' ? '/admin' : role === 'INSTRUCTOR' ? '/instructor/dashboard' : '/courses'

  return (
    <nav className="bg-gray-900 border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-6 h-14 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <Link to={home} className="text-white font-bold tracking-tight">
            Dev<span className="text-blue-500">Learn</span>
          </Link>

          {/* 관리자: 운영 메뉴만 */}
          {role === 'ADMIN' ? (
            <>
              <Link to="/admin" className="text-sm font-medium text-amber-400 hover:text-amber-300">
                관리자 콘솔
              </Link>
              <Link to="/admin/users" className="text-sm text-gray-300 hover:text-white">
                사용자
              </Link>
              <Link to="/admin/courses" className="text-sm text-gray-300 hover:text-white">
                강의
              </Link>
              <Link to="/admin/orders" className="text-sm text-gray-300 hover:text-white">
                주문
              </Link>
              <Link to="/admin/reports" className="text-sm text-gray-300 hover:text-white">
                신고
              </Link>
            </>
          ) : role === 'INSTRUCTOR' ? (
            /* 강사: 강사 콘솔 중심 (+ 다른 강의 수강을 위한 둘러보기·내 강의실) */
            <>
              <Link to="/instructor/dashboard" className="text-sm font-medium text-blue-400 hover:text-blue-300">
                강사 콘솔
              </Link>
              <Link to="/instructor/courses" className="text-sm text-gray-300 hover:text-white">
                내 강의 관리
              </Link>
              <Link to="/instructor/profile" className="text-sm text-gray-300 hover:text-white">
                강사 프로필
              </Link>
              <span className="text-gray-700">|</span>
              <Link to="/courses" className="text-sm text-gray-300 hover:text-white">
                강의 둘러보기
              </Link>
              <Link to="/my/courses" className="text-sm text-gray-300 hover:text-white">
                내 강의실
              </Link>
            </>
          ) : (
            /* 학생: 수강 메뉴 */
            <>
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
            </>
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
