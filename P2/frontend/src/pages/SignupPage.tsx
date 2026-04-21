import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../api/auth'

export default function SignupPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = (): string => {
    if (!/^[a-zA-Z0-9]{6,14}$/.test(username))
      return '아이디는 6~14자 영문, 숫자만 가능합니다.'
    if (password.length < 6 || password.length > 20)
      return '비밀번호는 6~20자여야 합니다.'
    if (password !== confirmPassword)
      return '비밀번호가 일치하지 않습니다.'
    return ''
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    const validationError = validate()
    if (validationError) {
      setError(validationError)
      return
    }
    setError('')
    setLoading(true)
    try {
      const { accessToken, refreshToken } = await signup(username, password)
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      navigate('/')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? '회원가입에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-10">
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Dev<span className="text-blue-500">Learn</span>
          </h1>
          <p className="mt-2 text-gray-400 text-sm">개발자를 위한 온라인 강의 플랫폼</p>
        </div>

        {/* Card */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8 shadow-xl">
          <h2 className="text-xl font-semibold text-white mb-6">회원가입</h2>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm text-gray-400 mb-1.5">아이디</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="6~14자 영문, 숫자"
                maxLength={14}
                required
                className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
              />
              <p className="mt-1 text-xs text-gray-600">영문, 숫자 조합 6~14자</p>
            </div>

            <div>
              <label className="block text-sm text-gray-400 mb-1.5">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="6~20자"
                maxLength={20}
                required
                className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition"
              />
            </div>

            <div>
              <label className="block text-sm text-gray-400 mb-1.5">비밀번호 확인</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="비밀번호 재입력"
                maxLength={20}
                required
                className={`w-full bg-gray-800 border text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-1 transition ${
                  confirmPassword && password !== confirmPassword
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500'
                    : 'border-gray-700 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {confirmPassword && password !== confirmPassword && (
                <p className="mt-1 text-xs text-red-400">비밀번호가 일치하지 않습니다.</p>
              )}
            </div>

            {error && (
              <p className="text-red-400 text-sm">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 disabled:cursor-not-allowed text-white font-medium rounded-lg py-2.5 text-sm transition"
            >
              {loading ? '가입 중...' : '회원가입'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-blue-400 hover:text-blue-300 transition">
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
