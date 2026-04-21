import api from './index'

export interface ReviewResponse {
  id: number
  userId: number
  courseId: number
  rating: number
  comment: string
  createdAt: string
}

export const getReviews = async (courseId: number): Promise<ReviewResponse[]> => {
  const { data } = await api.get(`/reviews/courses/${courseId}`)
  return data.data
}

export const createReview = async (courseId: number, rating: number, comment: string): Promise<ReviewResponse> => {
  const { data } = await api.post('/reviews', { courseId, rating, comment })
  return data.data
}

export const deleteReview = async (id: number): Promise<void> => {
  await api.delete(`/reviews/${id}`)
}
