import { useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi } from '../../api/admin'

export default function AdminDashboardPage() {
  const [pendingReports, setPendingReports] = useState<number | null>(null)

  useEffect(() => {
    adminApi
      .listReports({ status: 'PENDING', size: 1 })
      .then((p) => setPendingReports(p.totalElements))
      .catch(() => setPendingReports(null))
  }, [])

  return (
    <AdminLayout title="대시보드">
      <p className="text-sm text-gray-400 mb-6">
        플랫폼 운영 콘솔입니다. 사용자·강의·주문·신고를 관리합니다.
      </p>

      {pendingReports !== null && pendingReports > 0 ? (
        <div className="bg-amber-950/40 border border-amber-800/50 rounded-xl p-4 text-amber-300 text-sm">
          미처리 신고가 <strong>{pendingReports}</strong>건 있습니다. 신고 탭에서 확인하세요.
        </div>
      ) : (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-4 text-gray-400 text-sm">
          미처리 신고가 없습니다.
        </div>
      )}
    </AdminLayout>
  )
}
