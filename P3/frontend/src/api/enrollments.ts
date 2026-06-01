import api from './index'

export interface EnrollmentResponse {
  id: number
  courseId: number
  createdAt: string
}

export const enroll = async (courseId: number): Promise<EnrollmentResponse> => {
  const { data } = await api.post('/enrollments', { courseId })
  return data.data
}

export const getMyEnrollments = async (): Promise<EnrollmentResponse[]> => {
  const { data } = await api.get('/enrollments/me')
  return data.data
}

export const cancelEnrollment = async (id: number): Promise<void> => {
  await api.delete(`/enrollments/${id}`)
}
