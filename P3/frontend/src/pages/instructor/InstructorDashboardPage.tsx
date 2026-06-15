import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import NavBar from '../../components/NavBar'
import { instructorApi, type DashboardData } from '../../api/instructor'

export default function InstructorDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    instructorApi
      .dashboard()
      .then(setData)
      .catch(() => setError('대시보드 데이터를 불러오지 못했습니다.'))
  }, [])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-6xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-white">강사 대시보드</h1>
          <Link
            to="/instructor/courses/new"
            className="bg-blue-600 hover:bg-blue-500 text-white text-sm font-medium px-4 py-2 rounded-lg"
          >
            새 강의 만들기
          </Link>
        </div>

        {error && <p className="text-red-400 mb-4">{error}</p>}

        {data && (
          <>
            <div className="bg-linear-to-br from-emerald-900/40 to-gray-900 border border-emerald-800/40 rounded-2xl p-6 mb-6">
              <div className="text-sm text-emerald-300/80">내 강의 누적 매출</div>
              <div className="text-4xl font-bold text-emerald-300 mt-2">
                {data.totalRevenue.toLocaleString()}원
              </div>
              <div className="text-xs text-gray-500 mt-2">결제 완료(환불 제외) 기준</div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-10">
              <Card label="전체 강의" value={data.totalCourses} hint={`발행 ${data.publishedCourses} · 임시 ${data.draftCourses} · 보관 ${data.archivedCourses}`} />
              <Card label="누적 수강생" value={data.totalEnrollments} />
              <Card label="리뷰" value={data.totalReviews} />
              <Card label="평균 평점" value={data.averageRating.toFixed(1)} hint="5점 만점" />
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
              <h2 className="text-lg font-semibold text-white mb-4">최근 수강 신청</h2>
              {data.recentEnrollments.length === 0 ? (
                <p className="text-sm text-gray-500">아직 수강생이 없습니다.</p>
              ) : (
                <ul className="divide-y divide-gray-800">
                  {data.recentEnrollments.map((e) => (
                    <li key={e.enrollmentId} className="py-3 flex items-center justify-between text-sm">
                      <div>
                        <span className="text-white">{e.studentUsername ?? '-'}</span>
                        <span className="text-gray-500 mx-2">→</span>
                        <span className="text-gray-300">{e.courseTitle ?? '강의 #' + e.courseId}</span>
                      </div>
                      <span className="text-gray-600 text-xs">{new Date(e.enrolledAt).toLocaleString()}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div className="mt-6 flex gap-3">
              <Link to="/instructor/courses" className="text-sm text-blue-400 hover:text-blue-300">
                내 강의 관리 →
              </Link>
              <Link to="/instructor/profile" className="text-sm text-blue-400 hover:text-blue-300">
                강사 프로필 편집 →
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function Card({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-5">
      <div className="text-sm text-gray-500">{label}</div>
      <div className="text-3xl font-bold text-white mt-2">{value}</div>
      {hint && <div className="text-xs text-gray-600 mt-2">{hint}</div>}
    </div>
  )
}
