import api from './index'

export interface LectureResponse {
  id: number
  title: string
  videoUrl: string | null
  orderNum: number
}

export interface SectionResponse {
  id: number
  title: string
  orderNum: number
  lectures: LectureResponse[]
}

export interface CourseResponse {
  id: number
  instructorId: number | null
  categoryId: number
  title: string
  description: string | null
  difficulty: string | null
  instructorName: string | null
  price: number
}

export interface CourseDetailResponse extends CourseResponse {
  sections: SectionResponse[]
}

export interface CoursePageResponse {
  content: CourseResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CourseFilters {
  categoryId?: number
  difficulty?: string
  keyword?: string
  page?: number
  size?: number
}

export const getCourses = async (filters: CourseFilters = {}): Promise<CoursePageResponse> => {
  const params: Record<string, unknown> = { page: filters.page ?? 0, size: filters.size ?? 12 }
  if (filters.categoryId !== undefined) params.categoryId = filters.categoryId
  if (filters.difficulty) params.difficulty = filters.difficulty
  if (filters.keyword) params.keyword = filters.keyword
  const { data } = await api.get('/courses', { params })
  return data.data
}

export const getCourse = async (id: number): Promise<CourseDetailResponse> => {
  const { data } = await api.get(`/courses/${id}`)
  return data.data
}
