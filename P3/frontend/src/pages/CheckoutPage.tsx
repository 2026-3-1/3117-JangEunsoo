import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { loadTossPayments } from '@tosspayments/tosspayments-sdk'
import NavBar from '../components/NavBar'
import { getOrder, type Order } from '../api/order'
import { checkout } from '../api/payment'
import { useAuth } from '../context/useAuth'

const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY as string | undefined

export default function CheckoutPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const { userId } = useAuth()
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState('')
  const [paying, setPaying] = useState(false)

  useEffect(() => {
    if (!orderId) return
    getOrder(Number(orderId))
      .then(setOrder)
      .catch(() => setError('주문 정보를 불러오지 못했습니다.'))
  }, [orderId])

  // Toss 결제창 호출 (VITE_TOSS_CLIENT_KEY 설정 시)
  const handleTossPay = async () => {
    if (!order || !TOSS_CLIENT_KEY) return
    setPaying(true)
    setError('')
    try {
      const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY)
      const payment = tossPayments.payment({ customerKey: `user-${userId ?? 'guest'}` })
      const origin = window.location.origin
      await payment.requestPayment({
        method: 'CARD',
        amount: { currency: 'KRW', value: order.totalAmount },
        orderId: order.orderNo, // Toss 가맹점 주문 식별자
        orderName:
          order.items.length > 1
            ? `${order.items[0].courseTitle} 외 ${order.items.length - 1}건`
            : order.items[0]?.courseTitle ?? '강의 결제',
        // 성공 콜백에 내부 주문 id를 함께 전달 (Toss는 orderNo만 돌려주므로)
        successUrl: `${origin}/checkout/success?internalOrderId=${order.id}`,
        failUrl: `${origin}/checkout/fail`,
        card: { flowMode: 'DEFAULT' },
      })
    } catch (err: unknown) {
      // 사용자가 결제창을 닫으면 SDK가 reject — 조용히 복귀
      const msg = (err as { message?: string })?.message ?? ''
      if (!/cancel|close|중단|취소/i.test(msg)) setError('결제창을 여는 중 오류가 발생했습니다.')
      setPaying(false)
    }
  }

  // 모의 결제 (Toss 키 미설정 시 — 개발·시연 안전망)
  const handleMockPay = async () => {
    if (!order) return
    setPaying(true)
    setError('')
    try {
      await checkout(order.id)
      alert('결제가 완료되었습니다.')
      navigate(`/orders/${order.id}`)
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      if (code === 'ORDER_NOT_PAYABLE') setError('이미 결제되었거나 결제할 수 없는 주문입니다.')
      else setError('결제에 실패했습니다.')
    } finally {
      setPaying(false)
    }
  }

  const tossEnabled = !!TOSS_CLIENT_KEY

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-2xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">결제</h1>

        {error && <p className="text-red-400 mb-4 text-sm">{error}</p>}

        {order && (
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 space-y-4">
            <div>
              <p className="text-xs text-gray-500">주문번호</p>
              <p className="text-sm text-white">{order.orderNo}</p>
            </div>

            <div className="border-t border-gray-800 pt-4">
              <p className="text-sm font-medium text-gray-300 mb-3">주문 항목</p>
              <ul className="space-y-2">
                {order.items.map((item) => (
                  <li key={item.id} className="flex justify-between text-sm">
                    <span className="text-gray-300">{item.courseTitle}</span>
                    <span className="text-white">{item.price.toLocaleString()}원</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="border-t border-gray-800 pt-4 flex justify-between items-center">
              <span className="text-sm text-gray-400">총 결제 금액</span>
              <span className="text-2xl font-bold text-white">
                {order.totalAmount.toLocaleString()}원
              </span>
            </div>

            {!tossEnabled && (
              <div className="bg-yellow-900/40 border border-yellow-800 rounded-lg p-3 text-xs text-yellow-200">
                ※ Toss 클라이언트 키 미설정 — 모의 결제로 진행됩니다.
              </div>
            )}

            {order.status !== 'PENDING' ? (
              <p className="text-sm text-gray-400">현재 상태: {order.status} (결제 진행 불가)</p>
            ) : (
              <button
                onClick={tossEnabled ? handleTossPay : handleMockPay}
                disabled={paying}
                className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 text-white font-medium rounded-xl"
              >
                {paying ? '결제 진행 중...' : tossEnabled ? '결제하기' : '모의 결제 진행'}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
