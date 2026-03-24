import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCourses, type CourseResponse } from '../api/courses'

export default function CoursesPage() {
  const navigate = useNavigate()
  const [courses, setCourses] = useState<CourseResponse[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getCourses(undefined, page)
      .then((res) => {
        setCourses(res.content)
        setTotalPages(res.totalPages)
      })
      .catch(() => setError('강의 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [page])

  const handleLogout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      <header className="flex items-center justify-between px-8 py-4 border-b border-gray-800">
        <h1 className="text-xl font-bold text-white">DevLearn</h1>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-400 hover:text-white transition-colors"
        >
          로그아웃
        </button>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-10">
        <h2 className="text-2xl font-semibold mb-6">강의 목록</h2>

        {loading && (
          <p className="text-gray-400 text-center py-20">불러오는 중...</p>
        )}

        {error && (
          <p className="text-red-400 text-center py-20">{error}</p>
        )}

        {!loading && !error && courses.length === 0 && (
          <p className="text-gray-500 text-center py-20">등록된 강의가 없습니다.</p>
        )}

        <ul className="space-y-3">
          {courses.map((course) => (
            <li
              key={course.id}
              onClick={() => navigate(`/courses/${course.id}`)}
              className="flex items-center justify-between bg-gray-900 hover:bg-gray-800 transition-colors rounded-xl px-6 py-4 cursor-pointer"
            >
              <span className="font-medium">{course.title}</span>
              <span className="text-xs text-gray-500">카테고리 #{course.categoryId}</span>
            </li>
          ))}
        </ul>

        {totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-8">
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => setPage(i)}
                className={`w-8 h-8 rounded-lg text-sm ${
                  i === page
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
