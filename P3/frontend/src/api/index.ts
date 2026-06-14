import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL ?? '/api'

const api = axios.create({
  baseURL: API_BASE,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 로그인/회원가입/토큰갱신 요청의 401은 인터셉터가 가로채지 않는다.
// (가로채면 로그인 실패 시 페이지가 /login으로 리다이렉트되어 에러 메시지가 사라진다)
const isAuthEndpoint = (url?: string) =>
  !!url && (url.includes('/auth/login') || url.includes('/auth/signup') || url.includes('/auth/refresh'))

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint(original.url)) {
      original._retry = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken })
          const newAccessToken = data.data.accessToken
          localStorage.setItem('accessToken', newAccessToken)
          original.headers.Authorization = `Bearer ${newAccessToken}`
          return api(original)
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
        }
      } else {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
