// 관리자 페이지 공통 유틸 (컴포넌트 아닌 헬퍼는 별도 모듈로 분리 — react-refresh 규칙)

export function extractMessage(e: unknown): string {
  if (typeof e === 'object' && e && 'response' in e) {
    const resp = (e as { response?: { data?: { message?: string } } }).response
    if (resp?.data?.message) return resp.data.message
  }
  return '요청을 처리하지 못했습니다.'
}
