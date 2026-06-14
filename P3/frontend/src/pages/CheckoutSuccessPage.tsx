import NavBar from '../components/NavBar'

// Toss 결제 성공 리다이렉트 처리 페이지 (구현은 결제 연동 단계에서)
export default function CheckoutSuccessPage() {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      <NavBar />
      <div className="max-w-2xl mx-auto px-6 py-10">
        <h1 className="text-2xl font-bold text-white mb-6">결제 처리 중...</h1>
      </div>
    </div>
  )
}
