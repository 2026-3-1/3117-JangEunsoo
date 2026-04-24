import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { getOrder, type Order, type OrderStatus } from '../api/order'
import { refund } from '../api/payment'

const statusLabel: Record<OrderStatus, string> = {
  PENDING: '결제 대기',
  PAID: '결제 완료',
  CANCELLED: '취소됨',
  REFUNDED: '전체 환불',
  PARTIAL_REFUNDED: '부분 환불',
}

export default function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState('')
  const [refunding, setRefunding] = useState(false)
  const [info, setInfo] = useState('')

  const load = async () => {
    if (!orderId) return
    try {
      const data = await getOrder(Number(orderId))
      setOrder(data)
    } catch {
      setError('주문 정보를 불러오지 못했습니다.')
    }
  }

  useEffect(() => {
    load()
  }, [orderId])

  const handleRefundItem = async (itemId: number) => {
    if (!order) return
    if (!confirm('해당 강의를 환불하시겠습니까? 수강 권한이 즉시 해제됩니다.')) return
    setRefunding(true)
    setError('')
    setInfo('')
    try {
      const updated = await refund(order.id, [itemId], 'USER_REQUEST')
      setOrder(updated)
      setInfo('환불이 완료되었습니다.')
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      if (code === 'ORDER_NOT_REFUNDABLE') setError('환불할 수 없는 주문 상태입니다.')
      else if (code === 'INVALID_REFUND_ITEMS') setError('이미 환불된 항목입니다.')
      else setError('환불에 실패했습니다.')
    } finally {
      setRefunding(false)
    }
  }

  const handleRefundAll = async () => {
    if (!order) return
    if (!confirm('주문 전체를 환불하시겠습니까?')) return
    setRefunding(true)
    setError('')
    setInfo('')
    try {
      const updated = await refund(order.id, undefined, 'USER_REQUEST')
      setOrder(updated)
      setInfo('환불이 완료되었습니다.')
    } catch {
      setError('환불에 실패했습니다.')
    } finally {
      setRefunding(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-3xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">주문 상세</h1>

        {error && <p className="text-red-400 mb-4 text-sm">{error}</p>}
        {info && <p className="text-emerald-400 mb-4 text-sm">{info}</p>}

        {order && (
          <div className="space-y-4">
            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-gray-500">주문번호</p>
                  <p className="text-sm text-white font-mono">{order.orderNo}</p>
                </div>
                <span className="text-sm bg-gray-800 text-gray-200 px-3 py-1 rounded">
                  {statusLabel[order.status]}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs text-gray-500">주문일시</p>
                  <p className="text-gray-300">{new Date(order.createdAt).toLocaleString()}</p>
                </div>
                {order.paidAt && (
                  <div>
                    <p className="text-xs text-gray-500">결제일시</p>
                    <p className="text-gray-300">{new Date(order.paidAt).toLocaleString()}</p>
                  </div>
                )}
              </div>
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
              <p className="text-sm font-medium text-gray-300 mb-3">주문 항목</p>
              <ul className="divide-y divide-gray-800">
                {order.items.map((item) => (
                  <li key={item.id} className="py-3 flex items-center justify-between">
                    <div>
                      <p className="text-white text-sm">{item.courseTitle}</p>
                      {item.status === 'REFUNDED' && (
                        <p className="text-xs text-yellow-400 mt-1">환불됨</p>
                      )}
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-white text-sm">{item.price.toLocaleString()}원</span>
                      {item.status === 'ACTIVE' &&
                        (order.status === 'PAID' || order.status === 'PARTIAL_REFUNDED') && (
                          <button
                            onClick={() => handleRefundItem(item.id)}
                            disabled={refunding}
                            className="text-xs text-red-400 hover:text-red-300 disabled:text-gray-600"
                          >
                            환불
                          </button>
                        )}
                    </div>
                  </li>
                ))}
              </ul>

              <div className="border-t border-gray-800 mt-4 pt-4 space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">총 주문 금액</span>
                  <span className="text-white">{order.totalAmount.toLocaleString()}원</span>
                </div>
                {order.refundedAmount > 0 && (
                  <div className="flex justify-between text-yellow-400">
                    <span>환불 금액</span>
                    <span>-{order.refundedAmount.toLocaleString()}원</span>
                  </div>
                )}
                <div className="flex justify-between font-medium pt-2 border-t border-gray-800">
                  <span className="text-gray-300">실 결제 금액</span>
                  <span className="text-white">
                    {(order.totalAmount - order.refundedAmount).toLocaleString()}원
                  </span>
                </div>
              </div>

              {(order.status === 'PAID' || order.status === 'PARTIAL_REFUNDED') &&
                order.items.some((i) => i.status === 'ACTIVE') && (
                  <button
                    onClick={handleRefundAll}
                    disabled={refunding}
                    className="mt-4 w-full py-2 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded-lg text-sm"
                  >
                    전체 환불
                  </button>
                )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
