import api from './index'

export interface CourseResponse {
  id: number
  categoryId: number
  title: string
}

export interface CoursePageResponse {
  content: CourseResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const getCourses = async (categoryId?: number, page = 0): Promise<CoursePageResponse> => {
  const params: Record<string, unknown> = { page, size: 10 }
  if (categoryId !== undefined) params.categoryId = categoryId
  const { data } = await api.get('/courses', { params })
  return data.data
}

export const getCourse = async (id: number): Promise<CourseResponse> => {
  const { data } = await api.get(`/courses/${id}`)
  return data.data
}
