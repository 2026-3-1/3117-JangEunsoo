import api from './index'

export type PublishStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface InstructorCourse {
  id: number
  instructorId: number
  categoryId: number
  title: string
  description: string | null
  difficulty: string | null
  price: number
  publishStatus: PublishStatus
  publishedAt: string | null
}

export interface InstructorSection {
  id: number
  courseId: number
  title: string
  orderNum: number | null
}

export interface InstructorLecture {
  id: number
  sectionId: number
  title: string
  videoUrl: string | null
  orderNum: number | null
  durationSeconds: number | null
}

export interface DashboardData {
  totalCourses: number
  publishedCourses: number
  draftCourses: number
  archivedCourses: number
  totalEnrollments: number
  totalReviews: number
  averageRating: number
  totalRevenue: number
  recentEnrollments: Array<{
    enrollmentId: number
    courseId: number
    courseTitle: string | null
    studentId: number
    studentUsername: string | null
    enrolledAt: string
  }>
}

export interface CourseStudents {
  courseId: number
  totalLectures: number
  students: Array<{
    enrollmentId: number
    userId: number
    username: string | null
    enrolledAt: string
    completedLectures: number
    progressRate: number
  }>
}

export interface InstructorProfile {
  id: number
  userId: number
  username: string
  displayName: string
  bio: string | null
  careerYears: number | null
  profileImageUrl: string | null
}

export interface PublicInstructorProfile {
  userId: number
  username: string
  displayName: string
  bio: string | null
  careerYears: number | null
  profileImageUrl: string | null
  totalEnrollments: number
  totalReviews: number
  averageRating: number
  courses: Array<{
    id: number
    title: string
    description: string | null
    difficulty: string | null
    price: number
    enrollmentCount: number
    averageRating: number
  }>
}

const unwrap = <T,>(p: Promise<{ data: { data: T } }>) => p.then((r) => r.data.data)

export const instructorApi = {
  listCourses: (status?: PublishStatus) =>
    unwrap<InstructorCourse[]>(
      api.get('/instructor/courses', { params: status ? { status } : {} })
    ),
  getCourse: (id: number) =>
    unwrap<InstructorCourse>(api.get(`/instructor/courses/${id}`)),
  createCourse: (body: {
    categoryId: number
    title: string
    description?: string
    difficulty?: string
    price?: number
  }) => unwrap<InstructorCourse>(api.post('/instructor/courses', body)),
  updateCourse: (
    id: number,
    body: { categoryId: number; title: string; description?: string; difficulty?: string; price?: number }
  ) => unwrap<InstructorCourse>(api.put(`/instructor/courses/${id}`, body)),
  deleteCourse: (id: number) => api.delete(`/instructor/courses/${id}`),
  publishCourse: (id: number) =>
    unwrap<InstructorCourse>(api.post(`/instructor/courses/${id}/publish`)),
  archiveCourse: (id: number) => api.post(`/instructor/courses/${id}/archive`),

  listSections: (courseId: number) =>
    unwrap<InstructorSection[]>(api.get(`/instructor/courses/${courseId}/sections`)),
  createSection: (courseId: number, body: { title: string; orderNum?: number }) =>
    unwrap<InstructorSection>(api.post(`/instructor/courses/${courseId}/sections`, body)),
  updateSection: (courseId: number, sectionId: number, body: { title: string; orderNum?: number }) =>
    unwrap<InstructorSection>(api.put(`/instructor/courses/${courseId}/sections/${sectionId}`, body)),
  deleteSection: (courseId: number, sectionId: number) =>
    api.delete(`/instructor/courses/${courseId}/sections/${sectionId}`),

  listLectures: (courseId: number, sectionId: number) =>
    unwrap<InstructorLecture[]>(
      api.get(`/instructor/courses/${courseId}/sections/${sectionId}/lectures`)
    ),
  createLecture: (
    courseId: number,
    sectionId: number,
    body: { title: string; videoUrl?: string; orderNum?: number; durationSeconds?: number }
  ) =>
    unwrap<InstructorLecture>(
      api.post(`/instructor/courses/${courseId}/sections/${sectionId}/lectures`, body)
    ),
  updateLecture: (
    courseId: number,
    sectionId: number,
    lectureId: number,
    body: { title: string; videoUrl?: string; orderNum?: number; durationSeconds?: number }
  ) =>
    unwrap<InstructorLecture>(
      api.put(
        `/instructor/courses/${courseId}/sections/${sectionId}/lectures/${lectureId}`,
        body
      )
    ),
  deleteLecture: (courseId: number, sectionId: number, lectureId: number) =>
    api.delete(`/instructor/courses/${courseId}/sections/${sectionId}/lectures/${lectureId}`),

  dashboard: () => unwrap<DashboardData>(api.get('/instructor/dashboard')),
  courseStudents: (courseId: number) =>
    unwrap<CourseStudents>(api.get(`/instructor/courses/${courseId}/students`)),

  myProfile: () => unwrap<InstructorProfile>(api.get('/instructor/profile')),
  updateMyProfile: (body: {
    displayName: string
    bio?: string
    careerYears?: number
    profileImageUrl?: string
  }) => unwrap<InstructorProfile>(api.put('/instructor/profile', body)),

  publicProfile: (userId: number) =>
    unwrap<PublicInstructorProfile>(api.get(`/instructors/${userId}`)),
}
