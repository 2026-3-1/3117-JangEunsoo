import api from './index'

export type Role = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export interface MeResponse {
  userId: number
  username: string
  role: Role
}

export interface SignupPayload {
  username: string
  password: string
  role?: Role
  displayName?: string
  bio?: string
  careerYears?: number
}

export const login = async (username: string, password: string): Promise<AuthResponse> => {
  const { data } = await api.post('/auth/login', { username, password })
  return data.data
}

export const fetchMe = async (): Promise<MeResponse> => {
  const { data } = await api.get('/auth/me')
  return data.data
}

export const signup = async (payload: SignupPayload): Promise<AuthResponse> => {
  await api.post('/auth/signup', payload)
  return login(payload.username, payload.password)
}
