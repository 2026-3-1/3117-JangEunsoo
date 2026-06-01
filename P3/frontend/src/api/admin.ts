import api from './index'
import type { Role } from './auth'

const unwrap = <T,>(p: Promise<{ data: { data: T } }>) => p.then((r) => r.data.data)

export type PublishStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'PARTIAL_REFUNDED'
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED'
export type ReportTargetType = 'REVIEW' | 'QNA_QUESTION' | 'QNA_ANSWER'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AdminUser {
  id: number
  username: string
  role: Role
  active: boolean
  createdAt: string
}

export interface AdminCourse {
  id: number
  title: string
  instructorId: number
  instructorUsername: string | null
  publishStatus: PublishStatus
  price: number
  blocked: boolean
  blockedReason: string | null
  enrollmentCount: number
  publishedAt: string | null
}

export interface AdminOrderItem {
  id: number
  courseId: number
  courseTitleSnapshot: string
  priceSnapshot: number
  status: string
}

export interface AdminOrder {
  id: number
  orderNo: string
  userId: number
  username: string | null
  status: OrderStatus
  totalAmount: number
  refundedAmount: number
  paidAt: string | null
  createdAt: string
  items: AdminOrderItem[]
}

export interface SalesSummary {
  grossRevenue: number
  refundedAmount: number
  netRevenue: number
  paidOrderCount: number
  refundedOrderCount: number
  partialRefundedOrderCount: number
}

export interface AdminReport {
  id: number
  reporterId: number
  reporterUsername: string | null
  targetType: ReportTargetType
  targetId: number
  reason: string
  status: ReportStatus
  resolverNote: string | null
  createdAt: string
}

export const adminApi = {
  // 사용자
  listUsers: (params: { role?: Role; keyword?: string; page?: number; size?: number }) =>
    unwrap<Page<AdminUser>>(api.get('/admin/users', { params })),
  changeRole: (userId: number, role: Role) =>
    unwrap<AdminUser>(api.patch(`/admin/users/${userId}/role`, { role })),
  deactivateUser: (userId: number) =>
    unwrap<AdminUser>(api.post(`/admin/users/${userId}/deactivate`)),
  activateUser: (userId: number) =>
    unwrap<AdminUser>(api.post(`/admin/users/${userId}/activate`)),

  // 강의
  listCourses: (params: { status?: PublishStatus; keyword?: string; page?: number; size?: number }) =>
    unwrap<Page<AdminCourse>>(api.get('/admin/courses', { params })),
  blockCourse: (courseId: number, reason: string) =>
    unwrap<AdminCourse>(api.post(`/admin/courses/${courseId}/block`, { reason })),
  unblockCourse: (courseId: number) =>
    unwrap<AdminCourse>(api.post(`/admin/courses/${courseId}/unblock`)),

  // 주문/매출
  listOrders: (params: { status?: OrderStatus; page?: number; size?: number }) =>
    unwrap<Page<AdminOrder>>(api.get('/admin/orders', { params })),
  salesSummary: () => unwrap<SalesSummary>(api.get('/admin/orders/sales-summary')),
  forceRefund: (orderId: number, body?: { orderItemIds?: number[]; reason?: string }) =>
    unwrap<unknown>(api.post(`/admin/orders/${orderId}/refund`, body ?? {})),

  // 신고
  listReports: (params: { status?: ReportStatus; targetType?: ReportTargetType; page?: number; size?: number }) =>
    unwrap<Page<AdminReport>>(api.get('/admin/reports', { params })),
  resolveReport: (reportId: number, body: { deleteTarget: boolean; note?: string }) =>
    unwrap<AdminReport>(api.post(`/admin/reports/${reportId}/resolve`, body)),
  dismissReport: (reportId: number, note?: string) =>
    unwrap<AdminReport>(api.post(`/admin/reports/${reportId}/dismiss`, { note })),
}
