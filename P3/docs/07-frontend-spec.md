# 07. 프론트엔드 사양 — DevLearn P2

React 19 + Vite + TypeScript + Tailwind. 라우팅은 `react-router-dom` v7.

## 1. 라우팅 표 (`src/App.tsx`)

```tsx
<Routes>
  {/* 공개 */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/signup" element={<SignupPage />} />

  {/* 인증 필요 */}
  <Route element={<ProtectedRoute />}>
    <Route path="/courses" element={<CoursesPage />} />
    <Route path="/courses/:id" element={<CourseDetailPage />} />
    <Route path="/courses/:id/learn/:enrollmentId" element={<LearningPage />} />
    <Route path="/my/courses" element={<MyCoursesPage />} />
    <Route path="/my/bookmarks" element={<MyBookmarksPage />} />
    <Route path="/instructors/:userId" element={<InstructorPublicProfilePage />} />
    <Route path="/cart" element={<CartPage />} />
    <Route path="/checkout/:orderId" element={<CheckoutPage />} />
    <Route path="/orders" element={<OrdersPage />} />
    <Route path="/orders/:orderId" element={<OrderDetailPage />} />

    {/* INSTRUCTOR 전용 */}
    <Route element={<RoleGuard allow={['INSTRUCTOR']} />}>
      <Route path="/instructor/dashboard" element={<InstructorDashboardPage />} />
      <Route path="/instructor/courses" element={<InstructorCourseListPage />} />
      <Route path="/instructor/courses/new" element={<InstructorCourseEditorPage />} />
      <Route path="/instructor/courses/:id/edit" element={<InstructorCourseEditorPage />} />
      <Route path="/instructor/courses/:id/students" element={<InstructorCourseStudentsPage />} />
      <Route path="/instructor/profile" element={<InstructorProfileEditPage />} />
    </Route>
  </Route>

  <Route path="*" element={<Navigate to="/courses" replace />} />
</Routes>
```

## 2. 인증 컨텍스트 (`src/context/AuthContext.tsx`)

```ts
type Role = 'STUDENT' | 'INSTRUCTOR';

interface AuthState {
  userId: number | null;
  username: string | null;
  role: Role | null;
  loading: boolean;       // 초기 부트스트랩 중인지
  refresh: () => Promise<void>;   // fetchMe() 호출 → 상태 갱신
  setLoggedOut: () => void;       // localStorage 토큰 제거 + 상태 초기화
}
```

### 부트스트랩 로직

`App` 마운트 시 `localStorage.getItem('accessToken')`이 존재하면 `refresh()` → `GET /api/auth/me` 호출 → 응답으로 state 채움. 실패하면 토큰 제거 후 logged-out.

`loading: true`인 동안 ProtectedRoute는 스피너만 렌더(리다이렉트 금지) — 새로고침 시 깜빡임 방지.

## 3. 가드 컴포넌트

### `ProtectedRoute.tsx`

```tsx
function ProtectedRoute() {
  const { userId, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!userId) return <Navigate to="/login" replace />;
  return <Outlet />;
}
```

### `RoleGuard.tsx`

```tsx
interface Props { allow: Role[]; }
function RoleGuard({ allow }: Props) {
  const { role, loading } = useAuth();
  if (loading) return <Spinner />;
  if (!role || !allow.includes(role)) {
    return <Navigate to="/courses" replace />;
  }
  return <Outlet />;
}
```

## 4. NavBar (`components/NavBar.tsx`)

역할별 메뉴 조건부 렌더링:

| 상태 | 표시 메뉴 |
|------|----------|
| 로그아웃 | 로그인, 회원가입 |
| STUDENT 로그인 | 강의 둘러보기, 내 수강, 북마크, 장바구니, 주문, 로그아웃 |
| INSTRUCTOR 로그인 | 강의 둘러보기, 강사 콘솔(대시보드/내 강의/프로필), 내 수강, 장바구니, 로그아웃 |

장바구니 아이콘에는 `cart.itemCount` 뱃지 표시.

## 5. API 클라이언트 모듈 (`src/api/`)

모든 모듈은 공통 `axios` 인스턴스 사용:
- `baseURL: import.meta.env.VITE_API_URL` (예: `http://localhost:8080`)
- Request 인터셉터: localStorage의 `accessToken`을 Authorization 헤더에 자동 부착
- Response 인터셉터: 401 발생 시 `/api/auth/refresh` 자동 시도 (실패 시 logout 후 `/login` 이동)

| 파일 | 노출 함수 |
|------|----------|
| `auth.ts` | `login`, `signup`, `logout`, `refresh`, `fetchMe` |
| `courses.ts` | `fetchCourses(filter)`, `fetchCourseDetail(id)` |
| `categories.ts` | `fetchCategories()` |
| `enrollments.ts` | `enroll(courseId)`, `fetchMyEnrollments()`, `fetchProgressRate(enrollmentId)`, `cancelEnrollment(id)` |
| `progress.ts` | `completeLecture(enrollmentId, lectureId)` |
| `reviews.ts` | `createReview(req)`, `fetchReviews(courseId)`, `deleteReview(id)` |
| `cart.ts` | `fetchCart`, `addToCart(courseId)`, `removeFromCart(courseId)`, `clearCart` |
| `order.ts` | `createOrder()`, `fetchOrders(status?)`, `fetchOrder(id)` |
| `payment.ts` | `checkout(orderId, simulateFailure?)`, `refund(orderId, req)` |
| `playback.ts` | `upsertPosition(req)`, `fetchPosition(lectureId, enrollmentId)`, `resume(enrollmentId)` |
| `bookmark.ts` | `fetchBookmarks(lectureId?)`, `createBookmark(req)`, `updateBookmark(id, req)`, `deleteBookmark(id)` |
| `instructor.ts` | `dashboard`, `myCourses(status?)`, `getCourse(id)`, `createCourse`, `updateCourse`, `deleteCourse`, `publishCourse`, `archiveCourse`, `cancelCourse`, `createSection`, `updateSection`, `deleteSection`, `createLecture`, `updateLecture`, `deleteLecture`, `myProfile`, `updateProfile`, `publicProfile(userId)`, `students(courseId)` |

## 6. 페이지별 핵심 동작

### 6-1. `LoginPage` / `SignupPage`
- 회원가입 시 role 선택 라디오 (STUDENT/INSTRUCTOR). 기본 STUDENT
- 로그인 성공 → access/refresh localStorage 저장 → `/courses`로 navigate

### 6-2. `CoursesPage` (강의 둘러보기)
- 카테고리 필터, 난이도 필터, 검색어 input
- 카드 그리드. 카드 클릭 → `/courses/:id`
- **PUBLISHED만** 표시 (서버가 필터링)

### 6-3. `CourseDetailPage`
- 강의 정보 + 강사 카드(`InstructorCard`) + 섹션/강의차시 트리 + 리뷰 목록
- 버튼 분기:
  - 비로그인 → 로그인 유도
  - 이미 수강 중 → "강의실로" → `/courses/:id/learn/:enrollmentId`
  - 무료 강의 + 미수강 → "수강 신청" → `POST /api/enrollments` → 자동 navigate
  - 유료 강의 + 미수강 → "장바구니에 담기" + "바로 구매"
- 리뷰 작성 입력은 진도율 ≥ 80%일 때만 노출. 미달 시 안내 문구

### 6-4. `LearningPage` (강의실)
- 좌측: 섹션/강의차시 목록 (완료 ✓ 표시)
- 메인: video 플레이어
  - 페이지 진입 시 `resume(enrollmentId)` 호출 → `currentTime` 복원
  - 재생 중 5~10초마다 `upsertPosition` 호출
  - 영상 종료(`onEnded`) 시 `completeLecture` 호출
- 우측: 현재 강의차시의 북마크 목록 + 추가 폼

### 6-5. `MyCoursesPage`
- 내 enrollments 카드 리스트. 각 카드에 진도율 progress bar
- 진도율은 `fetchProgressRate(enrollmentId)` 병렬 호출 (또는 enrollment 응답에 포함하도록 백엔드 합치기)

### 6-6. `MyBookmarksPage`
- 전체 북마크 리스트 (강의차시 제목 + 시간 + 메모)
- 클릭 시 해당 강의실로 이동 (`?t=초` 쿼리로 재생 위치 힌트)

### 6-7. `InstructorPublicProfilePage`
- 강사 정보 헤더 + 해당 강사의 PUBLISHED 강의 카드 그리드

### 6-8. `CartPage`
- 장바구니 아이템 리스트 + 항목 제거
- 하단 합계 + "주문하기" 버튼 → `POST /api/orders` → `/checkout/:orderId`

### 6-9. `CheckoutPage`
- 주문 항목 요약 + 결제 수단 (Mock Card 단일)
- "결제하기" 버튼 → `POST /api/payments/checkout`
- 디버그용 "실패 시뮬레이션" 체크박스 → `simulateFailure: true`
- 성공 시 → `/orders/:orderId` (또는 `/my/courses`)
- 실패(`402 PAYMENT_FAILED`) 시 → 토스트 + 재시도 버튼

### 6-10. `OrdersPage` / `OrderDetailPage`
- 주문 목록(상태별 필터)
- 상세에서 환불 신청 가능 (PAID/PARTIAL_REFUNDED만):
  - "전체 환불" 또는 OrderItem 체크박스 → `POST /api/payments/refund/:orderId`

### 6-11. 강사 콘솔 페이지

| 페이지 | 화면 |
|--------|------|
| `InstructorDashboardPage` | 강의 수, 발행 수, 학생 수, 최근 등록 등 카드 위젯 |
| `InstructorCourseListPage` | 내 강의 표(상태 탭: DRAFT/PUBLISHED/ARCHIVED). 발행/아카이브/폐강 버튼 |
| `InstructorCourseEditorPage` | 강의 기본정보 폼 + 섹션/강의차시 트리 편집 (드래그&드롭은 학습용 범위 외) |
| `InstructorCourseStudentsPage` | 수강생 표(username, 진도율, 등록일) |
| `InstructorProfileEditPage` | displayName, bio, careerYears, profileImageUrl 폼 |

## 7. 에러 처리 UX

`axios` 응답 인터셉터에서 공통 처리:
- 401 → refresh 시도 → 실패 시 logout
- 403 → "권한이 없습니다" 토스트
- 404 → 페이지별 fallback ("강의를 찾을 수 없습니다" 등)
- 422 `REVIEW_PROGRESS_GATE` → form 옆에 인라인 메시지 (`currentProgressRate`/`requiredRate` 활용)
- 422 `CART_SNAPSHOT_INVALID` → "장바구니 가격이 변경되었습니다. 다시 시도해주세요." 토스트 + 장바구니 새로고침
- 402 `PAYMENT_FAILED` → 재시도 가능 모달

## 8. 스타일 / Tailwind

- 4.x utility 우선. 컴포넌트 추상화는 최소
- 색상 팔레트는 기본 + 커스텀 1~2색 정도
- 한글 폰트는 시스템 폰트 스택 사용 (별도 웹폰트 도입 안 함)

## 9. 환경 변수

| 키 | 예시 | 용도 |
|----|------|------|
| `VITE_API_URL` | `http://localhost:8080` | API base URL |

`.env.development` / `.env.production` 분리. Vite는 `VITE_` 접두 변수만 노출.

## 10. 빌드 / 실행

```bash
cd P2/frontend
npm install
npm run dev         # 개발 (Vite, port 5173)
npm run build       # 프로덕션 (dist/)
npm run preview     # 빌드 산출물 서버 (port 4173)
npm run type-check  # tsc --noEmit
```
