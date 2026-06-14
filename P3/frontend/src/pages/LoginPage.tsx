import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fetchMe, login } from '../api/auth'
import { useAuth } from '../context/useAuth'

export default function LoginPage() {
  const navigate = useNavigate()
  const { refresh } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { accessToken, refreshToken } = await login(username, password)
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      const me = await fetchMe()
      await refresh()
      navigate(me.role === 'INSTRUCTOR' ? '/instructor/dashboard' : '/courses')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '아이디 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Dev<span className="text-blue-500">Learn</span>
          </h1>
          <p className="mt-2 text-gray-400 text-sm">개발자를 위한 온라인 강의 플랫폼</p>
        </div>

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8 shadow-xl">
          <h2 className="text-xl font-semibold text-white mb-6">로그인</h2>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm text-gray-400 mb-1.5">아이디</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="6~14자 영문, 숫자"
                minLength={6}
                maxLength={14}
                required
                className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
              />
            </div>

            <div>
              <label className="block text-sm text-gray-400 mb-1.5">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="6~20자"
                minLength={6}
                maxLength={20}
                required
                className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
              />
            </div>

            {error && <p className="text-red-400 text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 disabled:cursor-not-allowed text-white font-medium rounded-lg py-2.5 text-sm transition"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            계정이 없으신가요?{' '}
            <Link to="/signup" className="text-blue-400 hover:text-blue-300 transition">
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
