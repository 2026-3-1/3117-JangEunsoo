import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import NavBar from '../components/NavBar'
import { clearCart, getCart, removeFromCart, type Cart } from '../api/cart'
import { createOrder } from '../api/order'

export default function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<Cart | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const load = async () => {
    try {
      const data = await getCart()
      setCart(data)
    } catch {
      setError('장바구니를 불러오지 못했습니다.')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const handleRemove = async (courseId: number) => {
    await removeFromCart(courseId)
    load()
  }

  const handleClear = async () => {
    if (!confirm('장바구니를 모두 비우시겠습니까?')) return
    await clearCart()
    load()
  }

  const handleCheckout = async () => {
    if (!cart || cart.items.length === 0) return
    setSubmitting(true)
    setError('')
    try {
      const order = await createOrder()
      navigate(`/checkout/${order.id}`)
    } catch (err: unknown) {
      const code = (err as { response?: { data?: { code?: string } } })?.response?.data?.code
      if (code === 'EMPTY_CART') setError('장바구니가 비어있습니다.')
      else if (code === 'ALREADY_ENROLLED') setError('이미 수강 중인 강의가 포함되어 있습니다.')
      else setError('주문 생성에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-4xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">장바구니</h1>

        {error && <p className="text-red-400 mb-4 text-sm">{error}</p>}

        {!cart || cart.items.length === 0 ? (
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-10 text-center">
            <p className="text-gray-400">장바구니가 비어있습니다.</p>
            <Link to="/courses" className="text-sm text-blue-400 hover:text-blue-300 mt-3 inline-block">
              강의 둘러보기 →
            </Link>
          </div>
        ) : (
          <>
            <div className="bg-gray-900 border border-gray-800 rounded-2xl divide-y divide-gray-800">
              {cart.items.map((item) => (
                <div key={item.id} className="p-4 flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <Link
                      to={`/courses/${item.courseId}`}
                      className="text-white font-medium hover:text-blue-300"
                    >
                      {item.courseTitle}
                    </Link>
                    {item.instructorId && (
                      <Link
                        to={`/instructors/${item.instructorId}`}
                        className="block text-xs text-blue-400 hover:text-blue-300 mt-1"
                      >
                        {item.instructorName}
                      </Link>
                    )}
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-white font-medium">{item.price.toLocaleString()}원</span>
                    <button
                      onClick={() => handleRemove(item.courseId)}
                      className="text-sm text-red-400 hover:text-red-300"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mt-4 flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-400">총 {cart.itemCount}개 강의</p>
                <p className="text-2xl font-bold text-white mt-1">
                  {cart.totalAmount.toLocaleString()}원
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleClear}
                  className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded-lg text-sm"
                >
                  비우기
                </button>
                <button
                  onClick={handleCheckout}
                  disabled={submitting}
                  className="px-6 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 text-white rounded-lg text-sm font-medium"
                >
                  {submitting ? '주문 생성 중...' : '결제하기'}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
