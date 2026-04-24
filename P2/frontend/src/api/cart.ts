import api from './index'

export interface CartItem {
  id: number
  courseId: number
  courseTitle: string
  instructorId: number | null
  instructorName: string | null
  price: number
  addedAt: string
}

export interface Cart {
  items: CartItem[]
  totalAmount: number
  itemCount: number
}

export const getCart = async (): Promise<Cart> => {
  const { data } = await api.get('/cart')
  return data.data
}

export const addToCart = async (courseId: number): Promise<CartItem> => {
  const { data } = await api.post('/cart/items', { courseId })
  return data.data
}

export const removeFromCart = async (courseId: number): Promise<void> => {
  await api.delete(`/cart/items/${courseId}`)
}

export const clearCart = async (): Promise<void> => {
  await api.delete('/cart')
}
