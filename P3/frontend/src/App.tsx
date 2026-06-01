import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import CoursesPage from './pages/CoursesPage'
import CourseDetailPage from './pages/CourseDetailPage'
import MyCoursesPage from './pages/MyCoursesPage'
import LearningPage from './pages/LearningPage'
import ProtectedRoute from './components/ProtectedRoute'
import RoleGuard from './components/RoleGuard'
import InstructorDashboardPage from './pages/instructor/InstructorDashboardPage'
import InstructorCourseListPage from './pages/instructor/InstructorCourseListPage'
import InstructorCourseEditorPage from './pages/instructor/InstructorCourseEditorPage'
import InstructorCourseStudentsPage from './pages/instructor/InstructorCourseStudentsPage'
import InstructorProfileEditPage from './pages/instructor/InstructorProfileEditPage'
import InstructorPublicProfilePage from './pages/InstructorPublicProfilePage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import OrdersPage from './pages/OrdersPage'
import OrderDetailPage from './pages/OrderDetailPage'
import MyBookmarksPage from './pages/MyBookmarksPage'
import CourseQnaPage from './pages/CourseQnaPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'
import AdminCoursesPage from './pages/admin/AdminCoursesPage'
import AdminOrdersPage from './pages/admin/AdminOrdersPage'
import AdminReportsPage from './pages/admin/AdminReportsPage'

function App() {
  return (
    <BrowserRouter>
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
    </BrowserRouter>
  )
}

export default App
