import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
// 전 라우트 게이트(소형)는 eager 유지
import ProtectedRoute from './components/ProtectedRoute'
import RoleGuard from './components/RoleGuard'
import PageLoader from './components/PageLoader'

// 페이지는 라우트별 코드 스플리팅 (React.lazy) — 진입 시점에 청크 로드
const LoginPage = lazy(() => import('./pages/LoginPage'))
const SignupPage = lazy(() => import('./pages/SignupPage'))
const CoursesPage = lazy(() => import('./pages/CoursesPage'))
const CourseDetailPage = lazy(() => import('./pages/CourseDetailPage'))
const MyCoursesPage = lazy(() => import('./pages/MyCoursesPage'))
const LearningPage = lazy(() => import('./pages/LearningPage'))
const InstructorDashboardPage = lazy(() => import('./pages/instructor/InstructorDashboardPage'))
const InstructorCourseListPage = lazy(() => import('./pages/instructor/InstructorCourseListPage'))
const InstructorCourseEditorPage = lazy(() => import('./pages/instructor/InstructorCourseEditorPage'))
const InstructorCourseStudentsPage = lazy(() => import('./pages/instructor/InstructorCourseStudentsPage'))
const InstructorProfileEditPage = lazy(() => import('./pages/instructor/InstructorProfileEditPage'))
const InstructorPublicProfilePage = lazy(() => import('./pages/InstructorPublicProfilePage'))
const CartPage = lazy(() => import('./pages/CartPage'))
const CheckoutPage = lazy(() => import('./pages/CheckoutPage'))
const CheckoutSuccessPage = lazy(() => import('./pages/CheckoutSuccessPage'))
const CheckoutFailPage = lazy(() => import('./pages/CheckoutFailPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const OrderDetailPage = lazy(() => import('./pages/OrderDetailPage'))
const MyBookmarksPage = lazy(() => import('./pages/MyBookmarksPage'))
const CourseQnaPage = lazy(() => import('./pages/CourseQnaPage'))
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'))
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage'))
const AdminCoursesPage = lazy(() => import('./pages/admin/AdminCoursesPage'))
const AdminOrdersPage = lazy(() => import('./pages/admin/AdminOrdersPage'))
const AdminReportsPage = lazy(() => import('./pages/admin/AdminReportsPage'))

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        <Route
          path="/courses"
          element={
            <ProtectedRoute>
              <CoursesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/courses/:id"
          element={
            <ProtectedRoute>
              <CourseDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/courses/:id/learn/:enrollmentId"
          element={
            <ProtectedRoute>
              <LearningPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my/courses"
          element={
            <ProtectedRoute>
              <MyCoursesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my/bookmarks"
          element={
            <ProtectedRoute>
              <MyBookmarksPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/instructors/:userId"
          element={
            <ProtectedRoute>
              <InstructorPublicProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/cart"
          element={
            <ProtectedRoute>
              <CartPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/checkout/:orderId"
          element={
            <ProtectedRoute>
              <CheckoutPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/checkout/success"
          element={
            <ProtectedRoute>
              <CheckoutSuccessPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/checkout/fail"
          element={
            <ProtectedRoute>
              <CheckoutFailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders/:orderId"
          element={
            <ProtectedRoute>
              <OrderDetailPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/courses/:id/qna"
          element={
            <ProtectedRoute>
              <CourseQnaPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <RoleGuard allow={['ADMIN']} fallback="/courses">
              <AdminDashboardPage />
            </RoleGuard>
          }
        />
        <Route
          path="/admin/users"
          element={
            <RoleGuard allow={['ADMIN']} fallback="/courses">
              <AdminUsersPage />
            </RoleGuard>
          }
        />
        <Route
          path="/admin/courses"
          element={
            <RoleGuard allow={['ADMIN']} fallback="/courses">
              <AdminCoursesPage />
            </RoleGuard>
          }
        />
        <Route
          path="/admin/orders"
          element={
            <RoleGuard allow={['ADMIN']} fallback="/courses">
              <AdminOrdersPage />
            </RoleGuard>
          }
        />
        <Route
          path="/admin/reports"
          element={
            <RoleGuard allow={['ADMIN']} fallback="/courses">
              <AdminReportsPage />
            </RoleGuard>
          }
        />

        <Route
          path="/instructor/dashboard"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorDashboardPage />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorCourseListPage />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/new"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorCourseEditorPage />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/:id/edit"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorCourseEditorPage />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/:id/students"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorCourseStudentsPage />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/profile"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorProfileEditPage />
            </RoleGuard>
          }
        />

        <Route path="*" element={<Navigate to="/courses" replace />} />
      </Routes>
      </Suspense>
    </BrowserRouter>
  )
}

export default App
