import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import NavBar from '../../components/NavBar'
import { instructorApi, type InstructorCourse, type PublishStatus } from '../../api/instructor'

const statusOptions: Array<{ value: '' | PublishStatus; label: string }> = [
  { value: '', label: '전체' },
  { value: 'DRAFT', label: '임시' },
  { value: 'PUBLISHED', label: '발행' },
  { value: 'ARCHIVED', label: '보관' },
]

const statusLabel: Record<PublishStatus, string> = {
  DRAFT: '임시',
  PUBLISHED: '발행',
  ARCHIVED: '보관',
}

const statusBadge: Record<PublishStatus, string> = {
  DRAFT: 'bg-gray-700 text-gray-300',
  PUBLISHED: 'bg-emerald-700 text-emerald-100',
  ARCHIVED: 'bg-yellow-800 text-yellow-100',
}

export default function InstructorCourseListPage() {
  const navigate = useNavigate()
  const [courses, setCourses] = useState<InstructorCourse[]>([])
  const [filter, setFilter] = useState<'' | PublishStatus>('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = async (s: '' | PublishStatus) => {
    setLoading(true)
    setError('')
    try {
      const data = await instructorApi.listCourses(s === '' ? undefined : s)
      setCourses(data)
    } catch {
      setError('강의 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(filter)
  }, [filter])

  const handleDelete = async (id: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return
    try {
      await instructorApi.deleteCourse(id)
      load(filter)
    } catch {
      alert('삭제에 실패했습니다.')
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-6xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-white">내 강의 관리</h1>
          <Link
            to="/instructor/courses/new"
            className="bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium px-4 py-2 rounded-lg"
          >
            새 강의 만들기
          </Link>
        </div>

        <div className="flex gap-2 mb-4">
          {statusOptions.map((opt) => (
            <button
              key={opt.value || 'all'}
              onClick={() => setFilter(opt.value)}
              className={`px-3 py-1.5 rounded-md text-sm border transition ${
                filter === opt.value
                  ? 'bg-blue-600 border-blue-500 text-white'
                  : 'bg-gray-900 border-gray-800 text-gray-300 hover:border-gray-700'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        {error && <p className="text-red-400 mb-4">{error}</p>}

        <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-950 text-gray-500 text-xs uppercase">
              <tr>
                <th className="text-left px-4 py-3">제목</th>
                <th className="text-left px-4 py-3">상태</th>
                <th className="text-left px-4 py-3">난이도</th>
                <th className="text-left px-4 py-3">가격</th>
                <th className="text-right px-4 py-3">관리</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {loading ? (
                <tr><td colSpan={5} className="text-center py-8 text-gray-500">불러오는 중...</td></tr>
              ) : courses.length === 0 ? (
                <tr><td colSpan={5} className="text-center py-8 text-gray-500">강의가 없습니다.</td></tr>
              ) : (
                courses.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-800/50">
                    <td className="px-4 py-3 text-white">{c.title}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-block px-2 py-0.5 rounded text-xs ${statusBadge[c.publishStatus]}`}>
                        {statusLabel[c.publishStatus]}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-400">{c.difficulty ?? '-'}</td>
                    <td className="px-4 py-3 text-gray-300">{c.price === 0 ? '무료' : `${c.price.toLocaleString()}원`}</td>
                    <td className="px-4 py-3 text-right space-x-2">
                      <button
                        onClick={() => navigate(`/instructor/courses/${c.id}/edit`)}
                        className="text-blue-400 hover:text-blue-300"
                      >
                        편집
                      </button>
                      <button
                        onClick={() => navigate(`/instructor/courses/${c.id}/students`)}
                        className="text-gray-300 hover:text-white"
                      >
                        수강생
                      </button>
                      <button onClick={() => handleDelete(c.id)} className="text-red-400 hover:text-red-300">
                        삭제
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
