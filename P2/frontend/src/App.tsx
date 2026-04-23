import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import CoursesPage from './pages/CoursesPage'
import CourseDetailPage from './pages/CourseDetailPage'
import MyCoursesPage from './pages/MyCoursesPage'
import LearningPage from './pages/LearningPage'
import ProtectedRoute from './components/ProtectedRoute'
import RoleGuard from './components/RoleGuard'
import InstructorPlaceholderPage from './pages/instructor/InstructorPlaceholderPage'

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
          path="/instructor/dashboard"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="강사 대시보드" />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="내 강의 관리" />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/new"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="새 강의 만들기" />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/:id/edit"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="강의 편집" />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/courses/:id/students"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="수강생 관리" />
            </RoleGuard>
          }
        />
        <Route
          path="/instructor/profile"
          element={
            <RoleGuard allow={['INSTRUCTOR']}>
              <InstructorPlaceholderPage title="강사 프로필 편집" />
            </RoleGuard>
          }
        />

        <Route path="*" element={<Navigate to="/courses" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
