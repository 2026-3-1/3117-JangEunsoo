# 08. 학생 기능 스펙 (P2에서의 변경점) — DevLearn P2

P2의 **주인공은 강사**지만, 학생 측에서도 "강사 정보 노출"과 "DRAFT 강의 은폐" 등 표면적 변경이 있다. 본 문서는 P1 학생 기능이 P2에서 어떻게 달라지는지 정리한다.

---

## 1. 원칙

| 원칙 | 설명 |
|------|------|
| **하위호환** | 기존 학생 엔드포인트는 경로·요청·응답 포맷을 유지. 필드 **추가만** 허용 |
| **강사 정보 노출** | 강의 목록/상세에 강사 프로필 링크를 노출. 커뮤니티성 UX 강화 |
| **DRAFT 은폐** | 학생은 `publish_status = PUBLISHED`인 강의만 본다. DRAFT/ARCHIVED는 **목록에도 상세에도** 등장하지 않음 |
| **역할 무관 참여** | 강사 계정으로도 수강/리뷰 가능. "강사 전용 UI"만 role로 가드되고, 학습 자체는 모두 허용 |

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

**변경 없음.** 기존 엔드포인트/응답/프론트 유지.

단 내 수강 목록의 각 강의 카드도 (3-3과 동일 로직으로) 강사 링크 노출.

---

## 4. 프론트 라우팅 변경

### 4-1. 기존 라우트 (P1 그대로 유지)

```
/login                                   LoginPage
/signup                                  SignupPage (role 선택 UI만 추가)
/courses                                 CoursesPage
/courses/:id                             CourseDetailPage
/courses/:id/learn/:enrollmentId         LearningPage
/my/courses                              MyCoursesPage
```

### 4-2. 신규 라우트 (P2)

```
/instructors/:userId                     InstructorPublicProfilePage     (public)

/instructor/dashboard                    InstructorDashboardPage         (RoleGuard=INSTRUCTOR)
/instructor/courses                      InstructorCourseListPage        (RoleGuard=INSTRUCTOR)
/instructor/courses/new                  InstructorCourseEditorPage      (RoleGuard=INSTRUCTOR)
/instructor/courses/:id/edit             InstructorCourseEditorPage      (RoleGuard=INSTRUCTOR)
/instructor/courses/:id/students         InstructorCourseStudentsPage    (RoleGuard=INSTRUCTOR)
/instructor/profile                      InstructorProfileEditPage       (RoleGuard=INSTRUCTOR)
```

### 4-3. `RoleGuard` 컴포넌트 (신규)

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

## 5. `AuthContext` 확장

### 5-1. P1 상태 (추정)

```ts
type AuthContext = {
  isAuthenticated: boolean
  accessToken: string | null
  login(token: string, refresh: string): void
  logout(): void
}
```

### 5-2. P2 상태 (목표)

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

## 6. 네비게이션 바 (조건부 렌더링)

| 메뉴 항목 | 비로그인 | STUDENT | INSTRUCTOR |
|----------|:-------:|:-------:|:----------:|
| 전체 강의 | ✅ | ✅ | ✅ |
| 내 수강 | — | ✅ | ✅ |
| 강사 콘솔 (대시보드) | — | — | ✅ |
| 강의 관리 | — | — | ✅ |
| 내 프로필 | — | ✅ | ✅ |
| 로그인 | ✅ | — | — |
| 로그아웃 | — | ✅ | ✅ |

---

## 7. 학생 관점에서 바뀐 것 한 줄 요약

> 강의 목록/상세에서 강사 이름이 **클릭 가능**해지고, 미발행 강의는 보이지 않으며, 내가 가입할 때 "강사로 시작하기" 선택지가 생긴다. 그 외 기존 흐름은 **완전히 동일하게** 동작한다.

---

## 8. 관련 문서

- 전체 엔드포인트 목록: [03-api-design.md](./03-api-design.md)
- 라우팅 가드 규칙과 보안: [09-security-design.md §3 권한 매트릭스](./09-security-design.md)
- 강사 측 페이지 구조: [07-instructor-api-spec.md §1 프론트 대응](./07-instructor-api-spec.md)
