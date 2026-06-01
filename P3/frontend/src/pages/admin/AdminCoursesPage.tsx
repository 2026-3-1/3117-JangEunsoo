import { useCallback, useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi, type AdminCourse, type PublishStatus } from '../../api/admin'
import { Pager, extractMessage } from './AdminUsersPage'

const statusLabel: Record<PublishStatus, string> = {
  DRAFT: '임시저장',
  PUBLISHED: '발행',
  ARCHIVED: '보관/차단',
}

export default function AdminCoursesPage() {
  const [courses, setCourses] = useState<AdminCourse[]>([])
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<PublishStatus | ''>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    adminApi
      .listCourses({ keyword: keyword || undefined, status: statusFilter || undefined, page })
      .then((p) => {
        setCourses(p.content)
        setTotalPages(p.totalPages)
      })
      .catch(() => setError('강의 목록을 불러오지 못했습니다.'))
  }, [keyword, statusFilter, page])

  useEffect(() => {
    load()
  }, [load])

  const block = async (c: AdminCourse) => {
    const reason = prompt(`"${c.title}" 차단 사유를 입력하세요`)
    if (!reason) return
    try {
      await adminApi.blockCourse(c.id, reason)
      load()
    } catch (e) {
      alert(extractMessage(e))
    }
  }

  const unblock = async (c: AdminCourse) => {
    if (!confirm(`"${c.title}" 차단을 해제할까요? (DRAFT로 전환)`)) return
    try {
      await adminApi.unblockCourse(c.id)
      load()
    } catch (e) {
      alert(extractMessage(e))
    }
  }

  return (
    <AdminLayout title="강의 모더레이션">
      <div className="flex gap-2 mb-4">
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setPage(0), load())}
          placeholder="강의 제목 검색"
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200 w-56"
        />
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as PublishStatus | '')
            setPage(0)
          }}
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
        >
          <option value="">전체 상태</option>
          <option value="DRAFT">임시저장</option>
          <option value="PUBLISHED">발행</option>
          <option value="ARCHIVED">보관/차단</option>
        </select>
      </div>

      {error && <p className="text-red-400 mb-4">{error}</p>}

      <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden">
        <table className="w-full text-sm">
          <thead className="text-gray-500 text-left">
            <tr className="border-b border-gray-800">
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">제목</th>
              <th className="px-4 py-3">강사</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">수강생</th>
              <th className="px-4 py-3 text-right">조치</th>
            </tr>
          </thead>
          <tbody>
            {courses.map((c) => (
              <tr key={c.id} className="border-b border-gray-800/60">
                <td className="px-4 py-3 text-gray-500">{c.id}</td>
                <td className="px-4 py-3 text-white">
                  {c.title}
                  {c.blocked && (
                    <span className="ml-2 text-xs text-red-400" title={c.blockedReason ?? ''}>
                      🚫 차단됨
                    </span>
                  )}
                </td>
                <td className="px-4 py-3 text-gray-400">{c.instructorUsername ?? `#${c.instructorId}`}</td>
                <td className="px-4 py-3 text-gray-400">{statusLabel[c.publishStatus]}</td>
                <td className="px-4 py-3 text-gray-400">{c.enrollmentCount}</td>
                <td className="px-4 py-3 text-right">
                  {c.blocked ? (
                    <button
                      onClick={() => unblock(c)}
                      className="px-2 py-1 text-xs bg-green-600/80 hover:bg-green-500 rounded text-white"
                    >
                      차단 해제
                    </button>
                  ) : (
                    <button
                      onClick={() => block(c)}
                      className="px-2 py-1 text-xs bg-red-600/80 hover:bg-red-500 rounded text-white"
                    >
                      차단
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {courses.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-600">
                  강의가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pager page={page} totalPages={totalPages} onChange={setPage} />
    </AdminLayout>
  )
}
