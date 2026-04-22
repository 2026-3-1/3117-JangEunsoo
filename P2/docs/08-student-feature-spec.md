# 08. 학생 기능 스펙 (P2에서의 변경점) — DevLearn P2

P2는 **강사 역할 도입**과 더불어 **학생의 수강 라이프사이클을 본격적으로 확장**한다: 장바구니·모의 결제·환불·이어듣기·북마크·리뷰 진도 게이트. 본 문서는 P1 학생 기능이 P2에서 어떻게 달라지는지, 그리고 신규 기능이 어떤 페이지/컴포넌트/상호작용으로 드러나는지를 정리한다.

**🆕 수강생 확장 기능 요약 (PDF 기반, 모의 결제)**
- **장바구니** — 수강 전 담기, 중복/이미 수강한 강의 추가 차단
- **모의 결제** — 토스/PG 연동 없음. 체크아웃 시 `SUCCESS` 고정, 주문·결제 레코드만 남김
- **주문/환불 내역** — 내역 조회 + 개별 주문 항목 부분 환불
- **이어듣기** — 마지막 재생 위치(초) 저장, 다음 진입 시 자동 복귀
- **북마크** — 강의 카드 즐겨찾기, 별도 페이지에서 모아보기
- **리뷰 80% 게이트** — 수강 진도율 80% 미만이면 리뷰 작성 차단 (422)

---

## 1. 원칙

| 원칙 | 설명 |
|------|------|
| **하위호환** | 기존 학생 엔드포인트는 경로·요청·응답 포맷을 유지. 필드 **추가만** 허용 |
| **강사 정보 노출** | 강의 목록/상세에 강사 프로필 링크를 노출. 커뮤니티성 UX 강화 |
| **DRAFT 은폐** | 학생은 `publish_status = PUBLISHED`인 강의만 본다. DRAFT/ARCHIVED는 **목록에도 상세에도** 등장하지 않음 |
| **역할 무관 참여** | 강사 계정으로도 수강/리뷰 가능. "강사 전용 UI"만 role로 가드되고, 학습 자체는 모두 허용 |
| **🆕 모의 결제 일관성** | 유료/무료 모두 **주문 흐름을 통과**. 0원 주문도 `orders`·`payments`에 레코드 남김. UI상 "결제" 버튼은 유료, "무료 수강하기"는 0원 단일 클릭 |
| **🆕 소유 리소스 은폐(404)** | 주문/북마크/재생 위치 등 학생 개인 리소스를 타인이 조회하면 **403 아닌 404** — 존재 자체를 숨김 |

---

## 2. P1 학생 기능 인벤토리 (현재 상태 복습)

| 기능 | 엔드포인트 | 프론트 페이지 |
|------|-----------|-------------|
| 회원가입 | `POST /api/auth/signup` | `SignupPage.tsx` |
| 로그인 | `POST /api/auth/login` | `LoginPage.tsx` |
| 강의 목록 | `GET /api/courses` | `CoursesPage.tsx` |
| 강의 상세 | `GET /api/courses/{id}` | `CourseDetailPage.tsx` |
| 수강 신청 | `POST /api/enrollments` | `CourseDetailPage.tsx` 내 버튼 |
| 내 수강 목록 | `GET /api/enrollments/me` | `MyCoursesPage.tsx` |
| 진도율 조회 | `GET /api/enrollments/{id}/progress-rate` | `MyCoursesPage.tsx` / `LearningPage.tsx` |
| 렉처 완료 처리 | `POST /api/progress/complete` | `LearningPage.tsx` |
| 수강 취소 | `DELETE /api/enrollments/{id}` | `MyCoursesPage.tsx` |
| 리뷰 작성 | `POST /api/reviews` | `CourseDetailPage.tsx` 내 폼 |
| 리뷰 조회 | `GET /api/reviews/courses/{courseId}` | `CourseDetailPage.tsx` |
| 리뷰 삭제 | `DELETE /api/reviews/{id}` | `CourseDetailPage.tsx` |

---

## 3. P2에서의 변경 사항 (학생 관점)

### 3-1. 회원가입 (`SignupPage`)

**변경점:**
- 역할 선택 UI 추가 (라디오: "학생으로 시작" / "강사로 시작")
- 강사 선택 시 `displayName`, `bio`, `careerYears` 폼 확장
- 기본값은 "학생" (role=STUDENT) — 기존 사용자 흐름 보존

**요청 페이로드 (변경):**

```json
{
  "username": "alice",
  "password": "P@ssw0rd!",
  "role": "STUDENT"
}
```

강사로 가입하는 경우:

```json
{
  "username": "teacher01",
  "password": "P@ssw0rd!",
  "role": "INSTRUCTOR",
  "displayName": "김강사",
  "bio": "10년차 백엔드 엔지니어",
  "careerYears": 10
}
```

### 3-2. 로그인 (`LoginPage`)

**변경점: 응답에는 변화 없음** (토큰만 반환). 로그인 직후 프론트가 `GET /api/auth/me`(또는 별도 신규 엔드포인트) 또는 JWT payload에서 userId를 꺼낸 뒤, **role은 `CustomUserDetailsService`가 제공하는 권위를 따르기 위해** 별도 "내 프로필" API로 확보.

**권고 신규 엔드포인트** (부가):

```
GET /api/auth/me
```
응답:
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": 5,
    "username": "alice",
    "role": "INSTRUCTOR"
  }
}
```

프론트는 로그인 직후 이를 호출하여 `AuthContext`에 role을 저장 → `RoleGuard`와 네비게이션에 활용.

### 3-3. 강의 목록 (`CoursesPage` / `GET /api/courses`)

**백엔드 변경:**
- `publish_status = 'PUBLISHED'` 필터 기본 적용
- 각 카드 응답 DTO에 `instructorId`, `instructorName`(= `InstructorProfile.display_name` 우선, 없으면 기존 `courses.instructor_name` fallback), `publishedAt` 추가

**프론트 변경:**
- 강의 카드의 강사 이름을 **링크로 변경** → `/instructors/:instructorId`로 이동
- 정렬 옵션에 "발행일 최신순"(`publishedAt DESC`) 추가

**응답 필드 증분 (기존 + 🆕)**

| 필드 | 기존 | 🆕 |
|------|------|-----|
| id | ✅ | |
| title | ✅ | |
| description | ✅ | |
| difficulty | ✅ | |
| categoryId | ✅ | |
| instructorName | ✅ | (로직만 변경) |
| instructorId | — | ✅ |
| publishedAt | — | ✅ |

### 3-4. 강의 상세 (`CourseDetailPage` / `GET /api/courses/{id}`)

**백엔드 변경:**
- DRAFT/ARCHIVED 강의는 **소유자(강사)** 아닌 한 404 반환 (은폐)
- 응답에 `instructor` 오브젝트 포함 (userId, displayName, bio, careerYears, profileImageUrl)

**프론트 변경:**
- 강의 제목 아래에 **강사 카드** 섹션 (이름·경력·소개 요약, 프로필 페이지로 링크)
- 나머지(섹션/렉처 목록, 수강 버튼, 리뷰)는 그대로

### 3-5. 강사 공개 프로필 페이지 🆕 (`/instructors/:userId`)

**신규 페이지** `InstructorPublicProfilePage.tsx`

**섹션 구성:**

```
┌───────────────────────────────────────────┐
│ [프로필 이미지]                            │
│ 김강사  |  10년차                           │
│ ─────────────────────────────────────────  │
│ 소개                                       │
│ 10년간 스프링과 MSA를 가르쳐 온 강사...    │
│ ─────────────────────────────────────────  │
│ 개설 강의 (3)                              │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│ │ 스프링부트│ │ JPA 실전 │ │ MSA 기초 │    │
│ │ 입문      │ │           │ │           │   │
│ └──────────┘ └──────────┘ └──────────┘     │
│ ─────────────────────────────────────────  │
│ 통계                                       │
│ 누적 수강생 142명 · 평균 평점 ★4.6 · 리뷰 37│
└───────────────────────────────────────────┘
```

**엔드포인트**: `GET /api/instructors/{userId}` (public, 03-api-design §3-5)

**규칙:**
- `userId`가 존재하지만 role=STUDENT인 사용자를 조회하면 **404** (강사 목록 노출 방지)
- PUBLISHED 강의만 노출. 수강 클릭 시 기존 `/courses/:id` 흐름 재사용

### 3-6. 수강 신청 / 내 수강 / 진도 / 리뷰

**부분 변경.** 경로·기본 흐름은 유지하되 아래 규칙이 더해진다.

- `POST /api/enrollments` — **무료 강의 전용** 직결 경로. 유료 강의에 호출하면 `400 COURSE_NOT_FREE`. 유료는 장바구니→주문→결제 흐름을 타야 함 (§4 참조).
- `POST /api/reviews` — 해당 강의의 내 진도율이 **80% 미만**이면 `422 REVIEW_PROGRESS_GATE`. 응답 body에 `currentProgressRate`·`requiredRate`(=80) 포함 → 프론트는 진행률 바를 강조 렌더
- `MyCoursesPage` — 각 카드에 "이어듣기" 버튼 노출 (§5-3). 클릭 시 `GET /api/playback/enrollments/:id/resume`로 마지막 재생 렉처·초 수령 후 `LearningPage`로 이동.
- 내 수강 목록의 강사 이름은 §3-3과 동일하게 프로필 링크.

---

## 4. 🆕 수강 라이프사이클 확장 (장바구니 · 모의 결제 · 환불)

### 4-1. 장바구니 (`CartPage`)

**신규 페이지**: `/cart`

**레이아웃 개요:**

```
┌─────────────────────────────────────────────────────────┐
│ 장바구니 (3)                                            │
├─────────────────────────────────────────────────────────┤
│ ☐ [썸네일] 스프링부트 입문    강사: 김강사   49,000원  X │
│ ☐ [썸네일] JPA 실전           강사: 김강사   69,000원  X │
│ ☐ [썸네일] React 기초         강사: 박강사   0원(무료) X │
├─────────────────────────────────────────────────────────┤
│ 선택 삭제                              소계: 118,000원  │
│                                        [결제하기 ▶]     │
└─────────────────────────────────────────────────────────┘
```

**백엔드 엔드포인트** (03-api-design §4):

| 메서드 | 경로 | 목적 |
|--------|------|------|
| `GET /api/cart` | 내 장바구니 조회 |
| `POST /api/cart/items` | 담기 (body: `{ courseId }`) |
| `DELETE /api/cart/items/{courseId}` | 개별 제거 |
| `DELETE /api/cart` | 전체 비우기 |

**UI 동작 규칙:**
- 강의 상세 페이지 "장바구니 담기" 버튼 → `POST /api/cart/items`
  - 이미 담긴 강의: `409 CART_DUPLICATE` → 토스트 "이미 장바구니에 있습니다"
  - 이미 수강한 강의: `409 ALREADY_ENROLLED` → 토스트 "이미 수강 중인 강의입니다"
  - DRAFT/ARCHIVED: `404 COURSE_NOT_FOUND`
- 담김 즉시 NavBar 장바구니 배지 `+1` (§7-2 메뉴 표)
- `CartPage`에서 "결제하기" 클릭 → `POST /api/orders` 호출 → 즉시 `/checkout/:orderId` 이동

### 4-2. 체크아웃 / 모의 결제 (`CheckoutPage`)

**신규 페이지**: `/checkout/:orderId`

**레이아웃 개요:**

```
┌─────────────────────────────────────────────────────────┐
│ 주문 확인                                               │
│ 주문번호: ORD-20260421-0007                              │
├─────────────────────────────────────────────────────────┤
│ 스프링부트 입문           49,000원                      │
│ JPA 실전                  69,000원                      │
│ React 기초                     0원                      │
├─────────────────────────────────────────────────────────┤
│                        합계:  118,000원                 │
│                                                         │
│ 결제 수단: [ MOCK_CARD ▼ ]  (모의 결제 — 실제 차감 없음) │
│                                                         │
│ [ ◀ 장바구니 ]            [ 결제하기 (모의) ▶ ]         │
└─────────────────────────────────────────────────────────┘
```

**동작 흐름:**

1. 진입 시 `GET /api/orders/:id` 호출 → 주문 상태가 `PENDING`이 아니면 "이미 결제됨" 안내 후 `/orders`로 리디렉트
2. "결제하기(모의)" 클릭 → `POST /api/payments/checkout { orderId }`
3. 백엔드는 `MockPaymentGateway`로 `SUCCESS`를 즉시 반환하고 트랜잭션 내에서
   - `payments` 레코드 생성 (`status=SUCCESS`, `method=MOCK_CARD`)
   - `orders.status = PAID`, `paid_at = now()`
   - `order_items` 각 항목 → `enrollments` 자동 생성
   - 해당 `cart_items` 삭제
4. 응답 수신 → `/orders/:id` 상세로 이동, 성공 배너 표시

**에러 케이스:**
- `409 ORDER_NOT_PAYABLE` — 이미 PAID/CANCELLED 상태 (재시도·중복 탭 방지)
- `422 CART_SNAPSHOT_INVALID` — 주문 생성 후 강의가 DELETED/ARCHIVED 된 경우 → 주문 자동 취소 안내

### 4-3. 주문 내역 (`OrdersPage`) / 상세 (`OrderDetailPage`)

**신규 페이지**: `/orders`, `/orders/:id`

**리스트 (OrdersPage):**

| 주문번호 | 일시 | 금액 | 상태 | |
|---------|------|------|------|-|
| ORD-20260421-0007 | 2026-04-21 13:42 | 118,000원 | **결제완료** | [상세 ▶] |
| ORD-20260418-0003 | 2026-04-18 10:00 |  49,000원 | 환불완료 | [상세 ▶] |
| ORD-20260410-0001 | 2026-04-10 21:17 |       0원 | 결제완료(무료) | [상세 ▶] |

**상세 (OrderDetailPage):**
- 주문 항목별 금액 · 스냅샷된 강의명 · 상태 (ENROLLED / REFUNDED)
- 결제 수단 (MOCK_CARD 고정), 결제 일시, 환불 금액
- 액션: `[부분 환불] / [전체 환불]` — PAID 상태일 때만 노출
- 환불 버튼 → 모달: 사유 선택 (`USER_REQUEST` 만 학생 선택 가능, 나머지는 시스템 전용)
- 환불 확정 → `POST /api/orders/:id/refund { reason, orderItemIds? }`
  - `orderItemIds` 미지정 시 **전체 환불**. `orders.status = REFUNDED`
  - 지정 시 **부분 환불**. 해당 항목만 `REFUNDED`, 주문은 `PARTIAL_REFUNDED`
  - 해당 enrollments가 **자동 삭제**되어 접근 차단 (학습 데이터도 같이 정리)

### 4-4. 환불 규칙 (학생 관점)

| 사유 코드 | 누가 트리거 | UI 위치 |
|----------|------------|---------|
| `USER_REQUEST` | 학생 | OrderDetailPage 환불 버튼 |
| `COURSE_CANCELLED` | 시스템(강사 폐강) | 자동. 학생은 알림 토스트로만 인지 (`/orders` 방문 시 상태 갱신) |
| `CAPACITY_EXCEEDED` | 시스템 | P2 범위 외 — 모델에만 존재, UI 미노출 |

**제약:**
- 이미 `REFUNDED`·`CANCELLED` 주문 항목은 버튼 비활성
- 진도율 상관없이 환불 가능(모의 결제 정책) — 실운영 정책은 P3 논의

---

## 5. 🆕 학습 경험 확장 (이어듣기 · 북마크 · 리뷰 게이트)

### 5-1. 이어듣기 (`LearningPage` 내부)

**엔드포인트** (03-api-design §6):
- `PUT /api/playback { enrollmentId, lectureId, positionSeconds }` — 주기적 업서트
- `GET /api/playback/enrollments/:id/resume` — 이어듣기 진입점

**UI 동작:**
- `LearningPage`에서 `video` 엘리먼트의 `timeupdate` 이벤트를 **10초마다** throttle → `PUT /api/playback`
- 렉처 이동 시 즉시 1회 flush
- 렉처 95% 초과 재생 시 자동 `POST /api/progress/complete` (P1 기존 로직 재사용)
- 페이지 재진입: `positionSeconds` 만큼 `video.currentTime` 설정

### 5-2. 북마크 (`MyBookmarksPage` + 카드 상 즐겨찾기 아이콘)

**신규 페이지**: `/my/bookmarks`

**엔드포인트** (03-api-design §7):

| 메서드 | 경로 | 비고 |
|--------|------|------|
| `GET /api/bookmarks` | 내 북마크 목록 |
| `POST /api/bookmarks` | 추가 (`{ courseId }`) |
| `DELETE /api/bookmarks/:courseId` | 제거 |

**UI 동작:**
- 강의 카드 우상단 ★ 아이콘 토글 (즉시 낙관적 업데이트 + 실패 시 롤백)
- 동일 강의 중복 POST 시 **멱등 처리** (서버가 기존 레코드 반환 또는 200)
- `MyBookmarksPage` — 북마크 강의 카드 그리드 + "제거" 버튼

### 5-3. 내 수강 목록 — 이어듣기 버튼 (`MyCoursesPage`)

**기존 카드 레이아웃 확장:**

```
┌─────────────────────────────────────────────┐
│ [썸네일] 스프링부트 입문                     │
│          진도율 ████████░░ 62%               │
│          마지막 학습: 섹션2 > 렉처3 @ 03:20  │
│          [ ▶ 이어듣기 ]   [ 처음부터 ]       │
└─────────────────────────────────────────────┘
```

- "이어듣기" → `GET /api/playback/enrollments/:id/resume`으로 `{ lectureId, positionSeconds }` 수령 → `/courses/:id/learn/:enrollmentId?lecture=:lectureId&t=:sec` 이동
- 재생 위치가 없으면 첫 렉처 0초
- "처음부터" → 쿼리 없이 이동, `LearningPage`가 첫 렉처 로드

### 5-4. 리뷰 작성 80% 게이트 (`CourseDetailPage` 내 폼)

**백엔드:** `POST /api/reviews`
- 진도율 < 80% → `422 REVIEW_PROGRESS_GATE`
```json
{
  "success": false,
  "status": 422,
  "error": {
    "code": "REVIEW_PROGRESS_GATE",
    "message": "강의를 80% 이상 수강해야 리뷰를 작성할 수 있습니다.",
    "currentProgressRate": 62,
    "requiredRate": 80
  }
}
```

**프론트 동작:**
- 리뷰 폼 진입 시 `GET /api/enrollments/:id/progress-rate`로 선조회 → 80% 미만이면 폼 자체를 잠그고 "리뷰는 강의 80% 이상 수강 후 작성 가능합니다 (현재 62%)" 안내
- 서버가 여전히 게이트를 때렸다면 토스트 + 진행률 바 하이라이트
- 내가 이미 수강 취소(enrollment 삭제)한 강의에는 폼 자체 비노출

---

## 6. 프론트 라우팅 변경

### 6-1. 기존 라우트 (P1 그대로 유지)

```
/login                                   LoginPage
/signup                                  SignupPage (role 선택 UI만 추가)
/courses                                 CoursesPage
/courses/:id                             CourseDetailPage
/courses/:id/learn/:enrollmentId         LearningPage
/my/courses                              MyCoursesPage
```

### 6-2. 신규 라우트 (P2)

**강사 콘솔 계열:**
```
/instructors/:userId                     InstructorPublicProfilePage     (public)

/instructor/dashboard                    InstructorDashboardPage         (RoleGuard=INSTRUCTOR)
/instructor/courses                      InstructorCourseListPage        (RoleGuard=INSTRUCTOR)
/instructor/courses/new                  InstructorCourseEditorPage      (RoleGuard=INSTRUCTOR)
/instructor/courses/:id/edit             InstructorCourseEditorPage      (RoleGuard=INSTRUCTOR)
/instructor/courses/:id/students         InstructorCourseStudentsPage    (RoleGuard=INSTRUCTOR)
/instructor/profile                      InstructorProfileEditPage       (RoleGuard=INSTRUCTOR)
```

**🆕 학생 확장 계열 (인증 필요, 역할 무관):**
```
/cart                                    CartPage                        (ProtectedRoute)
/checkout/:orderId                       CheckoutPage                    (ProtectedRoute)
/orders                                  OrdersPage                      (ProtectedRoute)
/orders/:orderId                         OrderDetailPage                 (ProtectedRoute)
/my/bookmarks                            MyBookmarksPage                 (ProtectedRoute)
```

> 🛡️ 이 라우트들은 `RoleGuard`가 필요 없음 — 강사도 학생처럼 장바구니·결제·이어듣기·북마크를 쓸 수 있다는 **역할 무관 참여** 원칙(§1)을 따른다.

### 6-3. `RoleGuard` 컴포넌트 (신규)

```tsx
// components/RoleGuard.tsx
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

type Role = 'STUDENT' | 'INSTRUCTOR'

export default function RoleGuard({
  allow,
  children,
}: {
  allow: Role | Role[]
  children: React.ReactNode
}) {
  const { isAuthenticated, role } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  const allowedList = Array.isArray(allow) ? allow : [allow]
  if (!role || !allowedList.includes(role)) {
    return <Navigate to="/courses" replace />
  }
  return <>{children}</>
}
```

---

## 7. `AuthContext` 확장

### 7-1. P1 상태 (추정)

```ts
type AuthContext = {
  isAuthenticated: boolean
  accessToken: string | null
  login(token: string, refresh: string): void
  logout(): void
}
```

### 7-2. P2 상태 (목표)

```ts
type Role = 'STUDENT' | 'INSTRUCTOR'

type AuthContext = {
  isAuthenticated: boolean
  userId: number | null
  role: Role | null
  accessToken: string | null
  login(token: string, refresh: string): Promise<void>   // login 시 /api/auth/me 호출
  logout(): void
  refresh(): Promise<void>
}
```

**구현 방식:**
- 로그인 성공 → 토큰 저장 → `GET /api/auth/me` 호출 → `userId`, `role` 세팅
- 새로고침 시 `localStorage`의 토큰이 있으면 동일하게 `/auth/me` 한 번 호출
- 네비게이션 바: `role === 'INSTRUCTOR'`일 때 "강사 콘솔" 메뉴 노출

---

## 8. 네비게이션 바 (조건부 렌더링)

### 8-1. 메뉴 노출 매트릭스

| 메뉴 항목 | 비로그인 | STUDENT | INSTRUCTOR |
|----------|:-------:|:-------:|:----------:|
| 전체 강의 | ✅ | ✅ | ✅ |
| 내 수강 | — | ✅ | ✅ |
| 🆕 장바구니 (배지) | — | ✅ | ✅ |
| 🆕 주문 내역 | — | ✅ | ✅ |
| 🆕 북마크 | — | ✅ | ✅ |
| 강사 콘솔 (대시보드) | — | — | ✅ |
| 강의 관리 | — | — | ✅ |
| 내 프로필 | — | ✅ | ✅ |
| 로그인 | ✅ | — | — |
| 로그아웃 | — | ✅ | ✅ |

### 8-2. 장바구니 배지

- NavBar 장바구니 아이콘 옆에 담긴 강의 개수 배지 렌더
- 카운트 소스: 로그인 직후 `GET /api/cart`로 최초 세팅, 이후 담기/제거 응답 기준 낙관적 업데이트
- 0일 때는 배지 숨김

---

## 9. 학생 관점에서 바뀐 것 요약

- (P2 공통) 강의 목록/상세에서 강사 이름이 **클릭 가능**해지고, 미발행 강의는 보이지 않으며, 가입 시 "강사로 시작하기" 선택지가 생긴다.
- (수강 확장) 유료 강의는 **장바구니 → 모의 결제** 흐름으로 수강하게 된다. 결제는 실제 차감 없이 기록만 남고, 내 **주문/환불 내역**을 따로 조회할 수 있다.
- (학습 확장) `MyCoursesPage`에 **이어듣기** 버튼이 생기고, 강의 카드에서 ★ 아이콘으로 **북마크**할 수 있다.
- (품질 게이트) 리뷰는 **진도 80% 이상** 달성한 학생만 쓸 수 있다.

---

## 10. 관련 문서

- 전체 엔드포인트 목록: [03-api-design.md](./03-api-design.md)
- 라우팅 가드 규칙과 보안: [09-security-design.md §3 권한 매트릭스](./09-security-design.md)
- 강사 측 페이지 구조: [07-instructor-api-spec.md §1 프론트 대응](./07-instructor-api-spec.md)
