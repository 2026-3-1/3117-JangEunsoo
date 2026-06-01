import api from './index'

export interface Bookmark {
  id: number
  lectureId: number
  lectureTitle: string | null
  courseId: number | null
  courseTitle: string | null
  timeSeconds: number
  memo: string | null
  createdAt: string
}

export const listMyBookmarks = async (): Promise<Bookmark[]> => {
  const { data } = await api.get('/bookmarks')
  return data.data
}

export const listBookmarksByLecture = async (lectureId: number): Promise<Bookmark[]> => {
  const { data } = await api.get('/bookmarks', { params: { lectureId } })
  return data.data
}

export const createBookmark = async (
  lectureId: number,
  timeSeconds: number,
  memo?: string
): Promise<Bookmark> => {
  const { data } = await api.post('/bookmarks', { lectureId, timeSeconds, memo })
  return data.data
}

export const updateBookmark = async (
  id: number,
  body: { timeSeconds?: number; memo?: string }
): Promise<Bookmark> => {
  const { data } = await api.put(`/bookmarks/${id}`, body)
  return data.data
}

export const deleteBookmark = async (id: number): Promise<void> => {
  await api.delete(`/bookmarks/${id}`)
}
