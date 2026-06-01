import { useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi, type SalesSummary } from '../../api/admin'

const won = (n: number) => `${n.toLocaleString()}원`

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<SalesSummary | null>(null)
  const [pendingReports, setPendingReports] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    adminApi.salesSummary().then(setSummary).catch(() => setError('매출 요약을 불러오지 못했습니다.'))
    adminApi
      .listReports({ status: 'PENDING', size: 1 })
      .then((p) => setPendingReports(p.totalElements))
      .catch(() => setPendingReports(null))
  }, [])

  return (
    <AdminLayout title="대시보드">
      {error && <p className="text-red-400 mb-4">{error}</p>}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <Card label="총 매출 (환불 전)" value={won(summary.grossRevenue)} />
          <Card label="누적 환불액" value={won(summary.refundedAmount)} />
          <Card label="순매출" value={won(summary.netRevenue)} accent />
          <Card
            label="결제 건수"
            value={`${summary.paidOrderCount}건`}
            hint={`환불 ${summary.refundedOrderCount} · 부분환불 ${summary.partialRefundedOrderCount}`}
          />
        </div>
      )}
      {pendingReports !== null && pendingReports > 0 && (
        <div className="bg-amber-950/40 border border-amber-800/50 rounded-xl p-4 text-amber-300 text-sm">
          미처리 신고가 <strong>{pendingReports}</strong>건 있습니다. 신고 탭에서 확인하세요.
        </div>
      )}
    </AdminLayout>
  )
}

function Card({
  label,
  value,
  hint,
  accent,
}: {
  label: string
  value: string
  hint?: string
  accent?: boolean
}) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-5">
      <div className="text-sm text-gray-500">{label}</div>
      <div className={`text-2xl font-bold mt-2 ${accent ? 'text-amber-400' : 'text-white'}`}>{value}</div>
      {hint && <div className="text-xs text-gray-600 mt-2">{hint}</div>}
    </div>
  )
}
