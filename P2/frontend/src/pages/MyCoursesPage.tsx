import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyEnrollments, type EnrollmentResponse } from '../api/enrollments'
import { getCourse, type CourseDetailResponse } from '../api/courses'
import { getProgressRate, type ProgressRateResponse } from '../api/progress'

interface EnrolledCourse {
  enrollment: EnrollmentResponse
  course: CourseDetailResponse
  progress: ProgressRateResponse | null
}

export default function MyCoursesPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<EnrolledCourse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true)
      try {
        const enrollments = await getMyEnrollments()
        const results = await Promise.all(
          enrollments.map(async (enrollment) => {
            const [course, progress] = await Promise.all([
              getCourse(enrollment.courseId),
              getProgressRate(enrollment.id).catch(() => null),
            ])
            return { enrollment, course, progress }
          })
        )
        setItems(results)
      } catch {
        setError('수강목록을 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }
    fetchAll()
  }, [])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <header className="flex items-center justify-between px-8 py-4 border-b border-gray-800">
        <button
          onClick={() => navigate('/courses')}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          ← 강의 목록
        </button>
        <h1 className="text-xl font-bold text-white">DevLearn</h1>
        <div className="w-20" />
      </header>

      <main className="max-w-4xl mx-auto px-6 py-10">
        <h2 className="text-2xl font-semibold mb-6">내 수강목록</h2>

        {loading && <p className="text-gray-400 text-center py-20">불러오는 중...</p>}
        {error && <p className="text-red-400 text-center py-20">{error}</p>}
        {!loading && !error && items.length === 0 && (
          <p className="text-gray-500 text-center py-20">수강 중인 강의가 없습니다.</p>
        )}

        <ul className="space-y-4">
          {items.map(({ enrollment, course, progress }) => {
            const rate = progress?.progressRate ?? 0
            return (
              <li
                key={enrollment.id}
                className="bg-gray-900 rounded-2xl px-6 py-5 space-y-3"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-white truncate">{course.title}</h3>
                    {course.instructorName && (
                      <p className="text-sm text-gray-400 mt-0.5">{course.instructorName}</p>
                    )}
                  </div>
                  <button
                    onClick={() => navigate(`/courses/${course.id}/learn/${enrollment.id}`)}
                    className="shrink-0 px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-xl text-sm font-medium transition-colors"
                  >
                    학습하기
                  </button>
                </div>

                <div className="space-y-1">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-400">진도율</span>
                    <span className="text-gray-300 font-medium">{rate}%</span>
                  </div>
                  <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-600 rounded-full transition-all duration-300"
                      style={{ width: `${rate}%` }}
                    />
                  </div>
                  {progress && (
                    <p className="text-xs text-gray-500">
                      {progress.completedLectures} / {progress.totalLectures} 강의 완료
                    </p>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      </main>
    </div>
  )
}
