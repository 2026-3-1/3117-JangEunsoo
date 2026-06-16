import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import NavBar from '../../components/NavBar'
import { instructorApi, type CourseStudents } from '../../api/instructor'

export default function InstructorCourseStudentsPage() {
  const { id } = useParams<{ id: string }>()
  const [data, setData] = useState<CourseStudents | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    instructorApi.courseStudents(Number(id)).then(setData).catch(() => setError('수강생 목록을 불러오지 못했습니다.'))
  }, [id])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-4xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-white">수강생 관리</h1>
          {id && (
            <Link
              to={`/courses/${id}/qna`}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-medium text-white"
            >
              강의 Q&amp;A 관리
            </Link>
          )}
        </div>

        {error && <p className="text-red-400 mb-4">{error}</p>}

        {data && (
          <>
            <p className="text-sm text-gray-500 mb-4">총 렉처 {data.totalLectures}개</p>
            <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-950 text-gray-500 text-xs uppercase">
                  <tr>
                    <th className="text-left px-4 py-3">학생</th>
                    <th className="text-left px-4 py-3">신청일</th>
                    <th className="text-left px-4 py-3">진도</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {data.students.length === 0 ? (
                    <tr><td colSpan={3} className="text-center py-8 text-gray-500">수강생이 없습니다.</td></tr>
                  ) : (
                    data.students.map((s) => (
                      <tr key={s.enrollmentId} className="hover:bg-gray-800/50">
                        <td className="px-4 py-3 text-white">{s.username ?? '#' + s.userId}</td>
                        <td className="px-4 py-3 text-gray-400">{new Date(s.enrolledAt).toLocaleDateString()}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <div className="flex-1 h-2 bg-gray-800 rounded overflow-hidden min-w-[120px]">
                              <div
                                className="h-2 bg-blue-500"
                                style={{ width: `${s.progressRate}%` }}
                              />
                            </div>
                            <span className="text-xs text-gray-400 w-12 text-right">
                              {s.progressRate}%
                            </span>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
