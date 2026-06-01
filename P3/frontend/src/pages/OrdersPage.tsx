import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { listMyOrders, type Order, type OrderStatus } from '../api/order'

const statusLabel: Record<OrderStatus, string> = {
  PENDING: '결제 대기',
  PAID: '결제 완료',
  CANCELLED: '취소됨',
  REFUNDED: '전체 환불',
  PARTIAL_REFUNDED: '부분 환불',
}

const statusColor: Record<OrderStatus, string> = {
  PENDING: 'bg-gray-700 text-gray-300',
  PAID: 'bg-emerald-700 text-emerald-100',
  CANCELLED: 'bg-gray-800 text-gray-400',
  REFUNDED: 'bg-yellow-800 text-yellow-100',
  PARTIAL_REFUNDED: 'bg-yellow-700 text-yellow-100',
}

const filters: Array<{ value: '' | OrderStatus; label: string }> = [
  { value: '', label: '전체' },
  { value: 'PENDING', label: '결제 대기' },
  { value: 'PAID', label: '결제 완료' },
  { value: 'REFUNDED', label: '환불' },
]

export default function OrdersPage() {
  const [filter, setFilter] = useState<'' | OrderStatus>('')
  const [orders, setOrders] = useState<Order[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    listMyOrders(filter === '' ? undefined : filter)
      .then(setOrders)
      .catch(() => setError('주문 내역을 불러오지 못했습니다.'))
  }, [filter])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-4xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">주문 내역</h1>

        <div className="flex gap-2 mb-4">
          {filters.map((f) => (
            <button
              key={f.value || 'all'}
              onClick={() => setFilter(f.value)}
              className={`px-3 py-1.5 rounded-md text-sm border transition ${
                filter === f.value
                  ? 'bg-blue-600 border-blue-500 text-white'
                  : 'bg-gray-900 border-gray-800 text-gray-300 hover:border-gray-700'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {error && <p className="text-red-400 mb-4 text-sm">{error}</p>}

        {orders.length === 0 ? (
          <p className="text-gray-500 text-sm">주문 내역이 없습니다.</p>
        ) : (
          <ul className="space-y-3">
            {orders.map((o) => (
              <li
                key={o.id}
                className="bg-gray-900 border border-gray-800 rounded-2xl p-5 hover:border-gray-700 transition"
              >
                <Link to={`/orders/${o.id}`} className="block">
                  <div className="flex items-center justify-between mb-2">
                    <div>
                      <p className="text-xs text-gray-500">{o.orderNo}</p>
                      <p className="text-xs text-gray-600">{new Date(o.createdAt).toLocaleString()}</p>
                    </div>
                    <span className={`text-xs px-2 py-1 rounded ${statusColor[o.status]}`}>
                      {statusLabel[o.status]}
                    </span>
                  </div>
                  <ul className="text-sm text-gray-300 space-y-1 mb-2">
                    {o.items.slice(0, 3).map((item) => (
                      <li key={item.id} className="truncate">
                        · {item.courseTitle}
                        {item.status === 'REFUNDED' && (
                          <span className="ml-2 text-xs text-yellow-400">환불됨</span>
                        )}
                      </li>
                    ))}
                    {o.items.length > 3 && (
                      <li className="text-xs text-gray-500">외 {o.items.length - 3}건</li>
                    )}
                  </ul>
                  <div className="flex justify-between items-center pt-2 border-t border-gray-800">
                    <span className="text-xs text-gray-500">총액</span>
                    <span className="text-white font-medium text-sm">
                      {o.totalAmount.toLocaleString()}원
                      {o.refundedAmount > 0 && (
                        <span className="ml-2 text-xs text-yellow-400">
                          (환불 {o.refundedAmount.toLocaleString()})
                        </span>
                      )}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
