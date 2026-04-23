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
