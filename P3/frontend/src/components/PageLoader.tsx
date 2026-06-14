// 라우트 코드 스플리팅(React.lazy)용 Suspense fallback
export default function PageLoader() {
  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <div className="flex flex-col items-center gap-3 text-gray-400">
        <div className="w-8 h-8 border-2 border-gray-700 border-t-blue-500 rounded-full animate-spin" />
        <span className="text-sm">불러오는 중...</span>
      </div>
    </div>
  )
}
