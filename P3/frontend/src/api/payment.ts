import api from './index'
import type { Order } from './order'

export type RefundReason = 'USER_REQUEST' | 'COURSE_CANCELLED' | 'DUPLICATE_PAYMENT' | 'OTHER'

export interface PaymentResult {
  id: number
  orderId: number
  method: 'MOCK_CARD'
  status: 'SUCCESS' | 'FAILED'
  amount: number
  mockTransactionId: string | null
  createdAt: string
}

export const checkout = async (orderId: number, simulateFailure = false): Promise<PaymentResult> => {
  const { data } = await api.post('/payments/checkout', { orderId }, { params: { simulateFailure } })
  return data.data
}

export const refund = async (
  orderId: number,
  orderItemIds?: number[],
  reason: RefundReason = 'USER_REQUEST'
): Promise<Order> => {
  const { data } = await api.post(`/payments/refund/${orderId}`, { orderItemIds, reason })
  return data.data
}
