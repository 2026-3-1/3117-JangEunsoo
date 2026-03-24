import api from './index'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
}

export const login = async (username: string, password: string): Promise<AuthResponse> => {
  const { data } = await api.post('/auth/login', { username, password })
  return data.data
}

export const signup = async (username: string, password: string): Promise<AuthResponse> => {
  const { data } = await api.post('/auth/signup', { username, password })
  return data.data
}
