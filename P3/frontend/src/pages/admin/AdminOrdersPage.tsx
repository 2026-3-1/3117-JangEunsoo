import { useCallback, useEffect, useState } from 'react'
import AdminLayout from './AdminLayout'
import { adminApi, type AdminOrder, type OrderStatus } from '../../api/admin'
import { Pager } from './AdminUsersPage'
import { extractMessage } from './adminUtils'

const won = (n: number) => `${n.toLocaleString()}원`
const statusLabel: Record<OrderStatus, string> = {
  PENDING: '결제대기',
  PAID: '결제완료',
  CANCELLED: '취소',
  REFUNDED: '환불완료',
  PARTIAL_REFUNDED: '부분환불',
}

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[]>([])
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    adminApi
      .listOrders({ status: statusFilter || undefined, page })
      .then((p) => {
        setOrders(p.content)
        setTotalPages(p.totalPages)
      })
      .catch(() => setError('주문 목록을 불러오지 못했습니다.'))
  }, [statusFilter, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const refund = async (o: AdminOrder) => {
    if (!confirm(`주문 ${o.orderNo} 전체 환불을 진행할까요? (수강 취소 포함)`)) return
    try {
      await adminApi.forceRefund(o.id, { reason: 'OTHER' })
      load()
    } catch (e) {
      alert(extractMessage(e))
    }
  }

  const refundable = (s: OrderStatus) => s === 'PAID' || s === 'PARTIAL_REFUNDED'

  return (
    <AdminLayout title="주문">
      <div className="flex gap-2 mb-4">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as OrderStatus | '')
            setPage(0)
          }}
          className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
        >
          <option value="">전체 상태</option>
          <option value="PAID">결제완료</option>
          <option value="REFUNDED">환불완료</option>
          <option value="PARTIAL_REFUNDED">부분환불</option>
          <option value="PENDING">결제대기</option>
          <option value="CANCELLED">취소</option>
        </select>
      </div>

      {error && <p className="text-red-400 mb-4">{error}</p>}

      <div className="space-y-3">
        {orders.map((o) => (
          <div key={o.id} className="bg-gray-900 border border-gray-800 rounded-xl p-4">
            <div className="flex items-center justify-between">
              <div className="text-sm">
                <span className="text-white font-medium">{o.orderNo}</span>
                <span className="text-gray-500 mx-2">·</span>
                <span className="text-gray-400">{o.username ?? `#${o.userId}`}</span>
                <span className="text-gray-500 mx-2">·</span>
                <span className="text-gray-400">{statusLabel[o.status]}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-white font-semibold">{won(o.totalAmount)}</span>
                {o.refundedAmount > 0 && (
                  <span className="text-xs text-red-400">(-{won(o.refundedAmount)})</span>
                )}
                {refundable(o.status) && (
                  <button
                    onClick={() => refund(o)}
                    className="px-2 py-1 text-xs bg-red-600/80 hover:bg-red-500 rounded text-white"
                  >
                    강제 환불
                  </button>
                )}
              </div>
            </div>
            <ul className="mt-2 text-xs text-gray-500">
              {(o.items ?? []).map((it) => (
                <li key={it.id}>
                  · {it.courseTitle} — {won(it.price)}{' '}
                  {it.status === 'REFUNDED' && <span className="text-red-400">[환불]</span>}
                </li>
              ))}
            </ul>
          </div>
        ))}
        {orders.length === 0 && <p className="text-center text-gray-600 py-8">주문이 없습니다.</p>}
      </div>

      <Pager page={page} totalPages={totalPages} onChange={setPage} />
    </AdminLayout>
  )
}
