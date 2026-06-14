import { useNavigate, useSearchParams } from 'react-router-dom'
import NavBar from '../components/NavBar'

// Toss 결제 실패 리다이렉트 처리 (failUrl로 code·message 전달됨)
export default function CheckoutFailPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const code = params.get('code')
  const message = params.get('message')

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-2xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-4">결제 실패</h1>
        <p className="text-red-400 text-sm mb-2">{message ?? '결제가 정상 처리되지 않았습니다.'}</p>
        {code && <p className="text-xs text-gray-500 mb-6">오류 코드: {code}</p>}
        <div className="flex gap-2">
          <button
            onClick={() => navigate('/cart')}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-sm"
          >
            장바구니로 돌아가기
          </button>
          <button
            onClick={() => navigate('/orders')}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg text-sm"
          >
            주문 내역
          </button>
        </div>
      </div>
    </div>
  )
}
