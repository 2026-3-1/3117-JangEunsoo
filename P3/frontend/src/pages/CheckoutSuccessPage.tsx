import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { confirmTossPayment } from '../api/payment'

// Toss 결제 성공 리다이렉트 → 백엔드 승인(confirm) 요청 처리
export default function CheckoutSuccessPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const done = useRef(false) // StrictMode 이중 실행 방지

  useEffect(() => {
    if (done.current) return
    done.current = true

    const paymentKey = params.get('paymentKey')
    const amount = params.get('amount')
    const internalOrderId = params.get('internalOrderId')

    if (!paymentKey || !amount || !internalOrderId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError('결제 정보가 올바르지 않습니다.')
      return
    }

    confirmTossPayment({
      orderId: Number(internalOrderId),
      paymentKey,
      amount: Number(amount),
    })
      .then(() => {
        navigate(`/orders/${internalOrderId}`, { replace: true })
      })
      .catch((err: unknown) => {
        const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
        if (code === 'PAYMENT_AMOUNT_MISMATCH') setError('결제 금액이 주문 금액과 일치하지 않습니다.')
        else if (code === 'ORDER_NOT_PAYABLE') setError('이미 결제되었거나 결제할 수 없는 주문입니다.')
        else setError('결제 승인에 실패했습니다.')
      })
  }, [params, navigate])

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-2xl mx-auto px-6 py-10">
        {error ? (
          <>
            <h1 className="text-2xl font-bold text-white mb-4">결제 승인 실패</h1>
            <p className="text-red-400 text-sm mb-6">{error}</p>
            <button
              onClick={() => navigate('/orders')}
              className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg text-sm"
            >
              주문 내역으로
            </button>
          </>
        ) : (
          <h1 className="text-2xl font-bold text-white">결제 승인 처리 중...</h1>
        )}
      </div>
    </div>
  )
}
