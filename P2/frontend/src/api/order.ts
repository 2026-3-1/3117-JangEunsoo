import api from './index'

export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'PARTIAL_REFUNDED'
export type OrderItemStatus = 'ACTIVE' | 'REFUNDED'

export interface OrderItem {
  id: number
  courseId: number
  courseTitle: string
  price: number
  status: OrderItemStatus
}

export interface Order {
  id: number
  orderNo: string
  status: OrderStatus
  totalAmount: number
  refundedAmount: number
  paidAt: string | null
  createdAt: string
  items: OrderItem[]
}

export const createOrder = async (): Promise<Order> => {
  const { data } = await api.post('/orders')
  return data.data
}

export const listMyOrders = async (status?: OrderStatus): Promise<Order[]> => {
  const { data } = await api.get('/orders', { params: status ? { status } : {} })
  return data.data
}

export const getOrder = async (id: number): Promise<Order> => {
  const { data } = await api.get(`/orders/${id}`)
  return data.data
}
