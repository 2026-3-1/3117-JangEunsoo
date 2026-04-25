import api from './index'

export interface PlaybackPosition {
  enrollmentId: number
  lectureId: number
  currentTimeSeconds: number
  updatedAt: string | null
}

export interface ResumeInfo {
  enrollmentId: number
  courseId: number
  lectureId: number | null
  currentTimeSeconds: number
}

export const updatePlayback = async (
  enrollmentId: number,
  lectureId: number,
  currentTimeSeconds: number
): Promise<PlaybackPosition> => {
  const { data } = await api.put('/playback', { enrollmentId, lectureId, currentTimeSeconds })
  return data.data
}

export const getPlaybackPosition = async (
  lectureId: number,
  enrollmentId: number
): Promise<PlaybackPosition> => {
  const { data } = await api.get(`/playback/lectures/${lectureId}`, { params: { enrollmentId } })
  return data.data
}

export const getResume = async (enrollmentId: number): Promise<ResumeInfo> => {
  const { data } = await api.get(`/playback/enrollments/${enrollmentId}/resume`)
  return data.data
}
