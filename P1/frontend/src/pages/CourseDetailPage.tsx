import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCourse, type CourseResponse } from '../api/courses'
import { enroll, getMyEnrollments } from '../api/enrollments'

export default function CourseDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [course, setCourse] = useState<CourseResponse | null>(null)
  const [enrolled, setEnrolled] = useState(false)
  const [loading, setLoading] = useState(true)
  const [enrolling, setEnrolling] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    Promise.all([getCourse(Number(id)), getMyEnrollments()])
      .then(([courseData, myEnrollments]) => {
        setCourse(courseData)
        setEnrolled(myEnrollments.some((e) => e.courseId === courseData.id))
      })
      .catch(() => setError('강의 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleEnroll = async () => {
    if (!course) return
    setEnrolling(true)
    try {
      await enroll(course.id)
      setEnrolled(true)
    } catch {
      setError('수강신청에 실패했습니다.')
    } finally {
      setEnrolling(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <p className="text-gray-400">불러오는 중...</p>
      </div>
    )
  }

  if (error || !course) {
    return (
      <div className="min-h-screen bg-gray-950 flex items-center justify-center">
        <p className="text-red-400">{error || '강의를 찾을 수 없습니다.'}</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <header className="flex items-center justify-between px-8 py-4 border-b border-gray-800">
        <button
          onClick={() => navigate('/courses')}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          ← 목록으로
        </button>
        <h1 className="text-xl font-bold text-white">DevLearn</h1>
        <div className="w-16" />
      </header>

      <main className="max-w-2xl mx-auto px-6 py-12">
        <div className="bg-gray-900 rounded-2xl p-8 space-y-6">
          <div className="space-y-2">
            <span className="text-xs text-gray-500">카테고리 #{course.categoryId}</span>
            <h2 className="text-2xl font-bold">{course.title}</h2>
          </div>

          <hr className="border-gray-800" />

          {enrolled ? (
            <div className="flex items-center gap-2 text-green-400">
              <span>✓</span>
              <span className="text-sm font-medium">수강 중인 강의입니다</span>
            </div>
          ) : (
            <button
              onClick={handleEnroll}
              disabled={enrolling}
              className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 disabled:text-blue-400 rounded-xl font-medium transition-colors"
            >
              {enrolling ? '신청 중...' : '수강신청'}
            </button>
          )}
        </div>
      </main>
    </div>
  )
}
