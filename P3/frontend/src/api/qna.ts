import api from './index'

const unwrap = <T,>(p: Promise<{ data: { data: T } }>) => p.then((r) => r.data.data)

export interface QnaAnswer {
  id: number
  questionId: number
  authorId: number
  authorUsername: string | null
  authorRole: string
  content: string
  createdAt: string
}

export interface QnaQuestion {
  id: number
  courseId: number
  authorId: number
  authorUsername: string | null
  title: string
  content: string | null
  isPrivate: boolean
  answered: boolean
  answerCount: number
  createdAt: string
  answers: QnaAnswer[] | null
}

export interface QnaQuestionPage {
  content: QnaQuestion[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const qnaApi = {
  listByCourse: (courseId: number, params?: { page?: number; size?: number }) =>
    unwrap<QnaQuestionPage>(api.get(`/qna/courses/${courseId}/questions`, { params })),
  createQuestion: (courseId: number, body: { title: string; content: string; isPrivate: boolean }) =>
    unwrap<QnaQuestion>(api.post(`/qna/courses/${courseId}/questions`, body)),
  getQuestion: (questionId: number) =>
    unwrap<QnaQuestion>(api.get(`/qna/questions/${questionId}`)),
  updateQuestion: (questionId: number, body: { title: string; content: string; isPrivate: boolean }) =>
    unwrap<QnaQuestion>(api.put(`/qna/questions/${questionId}`, body)),
  deleteQuestion: (questionId: number) => api.delete(`/qna/questions/${questionId}`),

  createAnswer: (questionId: number, content: string) =>
    unwrap<QnaAnswer>(api.post(`/qna/questions/${questionId}/answers`, { content })),
  updateAnswer: (answerId: number, content: string) =>
    unwrap<QnaAnswer>(api.put(`/qna/answers/${answerId}`, { content })),
  deleteAnswer: (answerId: number) => api.delete(`/qna/answers/${answerId}`),
}

export const reportApi = {
  create: (body: { targetType: string; targetId: number; reason: string }) =>
    unwrap<unknown>(api.post('/reports', body)),
}
