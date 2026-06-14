import { useCallback, useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi, type AdminReport, type ReportStatus, type ReportTargetType } from '../../api/admin'
import { Pager } from './AdminUsersPage'
import { extractMessage } from './adminUtils'

const statusLabel: Record<ReportStatus, string> = {
  PENDING: '미처리',
  RESOLVED: '처리완료',
  DISMISSED: '반려',
}
const targetLabel: Record<ReportTargetType, string> = {
  REVIEW: '리뷰',
  QNA_QUESTION: 'Q&A 질문',
  QNA_ANSWER: 'Q&A 답변',
}

export default function AdminReportsPage() {
  const [reports, setReports] = useState<AdminReport[]>([])
  const [statusFilter, setStatusFilter] = useState<ReportStatus | ''>('PENDING')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    adminApi
      .listReports({ status: statusFilter || undefined, page })
      .then((p) => {
        setReports(p.content)
        setTotalPages(p.totalPages)
      })
      .catch(() => setError('신고 목록을 불러오지 못했습니다.'))
  }, [statusFilter, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const resolve = async (r: AdminReport, deleteTarget: boolean) => {
    const note = prompt(deleteTarget ? '대상을 삭제하고 처리합니다. 메모(선택):' : '대상을 유지하고 처리합니다. 메모(선택):')
    if (note === null) return
    try {
      await adminApi.resolveReport(r.id, { deleteTarget, note: note || undefined })
      load()
    } catch (e) {
      alert(extractMessage(e))
    }
  }

  const dismiss = async (r: AdminReport) => {
    const note = prompt('반려 메모(선택):')
    if (note === null) return
    try {
      await adminApi.dismissReport(r.id, note || undefined)
      load()
    } catch (e) {
      alert(extractMessage(e))
    }
  }

  return (
    <AdminLayout title="신고 모더레이션">
      <div className="flex gap-2 mb-4">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as ReportStatus | '')
            setPage(0)
          }}
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
        >
          <option value="PENDING">미처리</option>
          <option value="RESOLVED">처리완료</option>
          <option value="DISMISSED">반려</option>
          <option value="">전체</option>
        </select>
      </div>

      {error && <p className="text-red-400 mb-4">{error}</p>}

      <div className="space-y-3">
        {reports.map((r) => (
          <div key={r.id} className="bg-gray-900 border border-gray-800 rounded-xl p-4">
            <div className="flex items-center justify-between">
              <div className="text-sm">
                <span className="text-amber-400">{targetLabel[r.targetType]} #{r.targetId}</span>
                <span className="text-gray-500 mx-2">·</span>
                <span className="text-gray-400">신고자 {r.reporterUsername ?? `#${r.reporterId}`}</span>
                <span className="text-gray-500 mx-2">·</span>
                <span className="text-gray-500">{statusLabel[r.status]}</span>
              </div>
              <span className="text-xs text-gray-600">{new Date(r.createdAt).toLocaleString()}</span>
            </div>
            <p className="mt-2 text-sm text-gray-300">{r.reason}</p>
            {r.resolverNote && <p className="mt-1 text-xs text-gray-500">처리 메모: {r.resolverNote}</p>}
            {r.status === 'PENDING' && (
              <div className="mt-3 flex gap-2">
                <button
                  onClick={() => resolve(r, true)}
                  className="px-2 py-1 text-xs bg-red-600/80 hover:bg-red-500 rounded text-white"
                >
                  대상 삭제 후 처리
                </button>
                <button
                  onClick={() => resolve(r, false)}
                  className="px-2 py-1 text-xs bg-blue-600/80 hover:bg-blue-500 rounded text-white"
                >
                  유지하고 처리
                </button>
                <button
                  onClick={() => dismiss(r)}
                  className="px-2 py-1 text-xs bg-gray-700 hover:bg-gray-600 rounded text-white"
                >
                  반려
                </button>
              </div>
            )}
          </div>
        ))}
        {reports.length === 0 && <p className="text-center text-gray-600 py-8">신고가 없습니다.</p>}
      </div>

      <Pager page={page} totalPages={totalPages} onChange={setPage} />
    </AdminLayout>
  )
}
