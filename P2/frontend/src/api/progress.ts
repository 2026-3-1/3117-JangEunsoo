import api from './index'

export interface ProgressRateResponse {
  enrollmentId: number
  courseId: number
  totalLectures: number
  completedLectures: number
  progressRate: number
}

export const completeLecture = async (enrollmentId: number, lectureId: number): Promise<void> => {
  await api.post('/progress/complete', { enrollmentId, lectureId })
}

export const getProgressRate = async (enrollmentId: number): Promise<ProgressRateResponse> => {
  const { data } = await api.get(`/enrollments/${enrollmentId}/progress-rate`)
  return data.data
}
