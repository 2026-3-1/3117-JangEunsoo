# 04. 아키텍처 — DevLearn P2

P2의 아키텍처는 **P1의 레이어드 구조를 그대로 계승**하되, 보안 필터 체인과 프론트 라우팅 가드에 **역할 기반 분기**가 추가된다. 본 문서는 기술 스택, 디렉토리 구조, 요청 흐름, 프론트 보호 라우트 전략을 정리한다.

---

## 1. 기술 스택

### 1-1. Backend

| 영역 | P1 | P2 추가/변경 |
|------|-----|-------------|
| Language | Java 21 | 동일 |
| Framework | Spring Boot 4.0.2 | 동일 |
| Build | Gradle (Kotlin DSL) | 동일 |
| Persistence | Spring Data JPA + Hibernate | 동일 |
| DB | MySQL 8.x | 동일 (스키마만 확장) |
| Auth | Spring Security + JWT (HS256) | `@EnableMethodSecurity` 활용 강화, `@PreAuthorize` 도입 |
| Password | BCrypt | 동일 |
| Validation | Jakarta Bean Validation | 동일 |
| Logging | SLF4J + Logback | 동일 |
| Test | JUnit 5 + Mockito | 권한 우회 E2E 테스트 추가 |

### 1-2. Frontend

| 영역 | P1 | P2 추가/변경 |
|------|-----|-------------|
| Language | TypeScript | 동일 |
| Framework | React 18 (또는 19) | 동일 |
| Build | Vite | 동일 |
| Styling | Tailwind CSS 4 | 동일 |
| Routing | React Router 7 | `RoleGuard` 컴포넌트 추가 |
| HTTP | Axios | 동일 |
| State | Context API | `AuthContext`에 role 필드 확장 |

### 1-3. Infra (개발)

| 항목 | 설정 |
|------|------|
| DB 마이그레이션 | 없음 (`spring.jpa.hibernate.ddl-auto=update`) + 수동 SQL |
| CORS | `http://localhost:5173` 허용 |
| JWT secret | 환경 변수 `JWT_SECRET_KEY` |
| Access token TTL | 1시간 |
| Refresh token TTL | 14일 (DB 저장, rotation) |

---

## 2. 모노레포 구조

```
d:/devlearn/
├── P1/
│   ├── backend/              ← P1 백엔드 (참고용, 수정 X)
│   ├── frontend/             ← P1 프론트 (참고용, 수정 X)
│   └── docs/, tmp/
└── P2/
    ├── backend/              ← P1에서 복사한 작업 트리
    ├── frontend/             ← P1에서 복사한 작업 트리
    ├── docs/                 ← 본 PRD 문서 세트
    └── CLAUDE.md
```

> P1 코드를 `P2/`로 복사하고, P2 범위 변경 사항만 P2 트리에 반영한다. P1은 **참고용 스냅샷**이므로 수정 금지.

---

## 3. Backend 디렉토리 구조

### 3-1. 전체 트리 (P2 기준)

```
P2/backend/src/main/java/com/jes/devlearn/
├── DevlearnApplication.java
├── domain/
│   ├── user/
│   │   ├── entity/
│   │   │   ├── User.java                      ← role 필드 추가
│   │   │   └── Role.java                      🆕 enum STUDENT | INSTRUCTOR
│   │   ├── repository/UserRepository.java
│   │   ├── service/UserService.java
│   │   └── controller/AuthController.java     ← signup role 처리, /me 신규
│   ├── cart/                                  🆕
│   │   ├── entity/CartItem.java
│   │   ├── repository/CartItemRepository.java
│   │   ├── service/CartService.java
│   │   ├── controller/CartController.java
│   │   └── dto/
│   ├── order/                                 🆕
│   │   ├── entity/{Order, OrderItem}.java
│   │   ├── repository/
│   │   ├── service/OrderService.java
│   │   ├── controller/OrderController.java
│   │   └── dto/
│   ├── payment/                               🆕 (모의 결제)
│   │   ├── entity/{Payment, Refund}.java
│   │   ├── repository/
│   │   ├── service/
│   │   │   ├── PaymentService.java
│   │   │   ├── MockPaymentGateway.java        ← 토스 대체 모의
│   │   │   └── RefundService.java
│   │   ├── controller/PaymentController.java
│   │   └── dto/
│   ├── playback/                              🆕
│   │   ├── entity/PlaybackPosition.java
│   │   ├── repository/
│   │   ├── service/PlaybackService.java
│   │   ├── controller/PlaybackController.java
│   │   └── dto/
│   ├── bookmark/                              🆕
│   │   ├── entity/Bookmark.java
│   │   ├── repository/
│   │   ├── service/BookmarkService.java
│   │   ├── controller/BookmarkController.java
│   │   └── dto/
│   ├── instructor/                            🆕 전체 신규 패키지
│   │   ├── entity/
│   │   │   └── InstructorProfile.java
│   │   ├── repository/InstructorProfileRepository.java
│   │   ├── service/
│   │   │   ├── InstructorProfileService.java
│   │   │   ├── InstructorCourseService.java
│   │   │   ├── InstructorSectionService.java
│   │   │   ├── InstructorLectureService.java
│   │   │   └── InstructorDashboardService.java
│   │   ├── controller/
│   │   │   ├── InstructorCourseController.java
│   │   │   ├── InstructorSectionController.java
│   │   │   ├── InstructorLectureController.java
│   │   │   ├── InstructorDashboardController.java
│   │   │   ├── InstructorProfileController.java
│   │   │   └── InstructorPublicController.java  ← /api/instructors/{userId}
│   │   └── dto/
│   │       ├── CreateCourseRequest.java
│   │       ├── UpdateCourseRequest.java
│   │       ├── CreateSectionRequest.java
│   │       ├── CreateLectureRequest.java
│   │       ├── InstructorCourseResponse.java
│   │       ├── InstructorDashboardResponse.java
│   │       └── ...
│   ├── course/
│   │   ├── entity/
│   │   │   ├── Course.java                    ← instructor_id FK, publish_status 추가
│   │   │   ├── PublishStatus.java             🆕 enum
│   │   │   ├── Section.java
│   │   │   └── Lecture.java
│   │   ├── repository/
│   │   │   ├── CourseRepository.java          ← PUBLISHED 필터 메서드 추가
│   │   │   ├── SectionRepository.java
│   │   │   └── LectureRepository.java
│   │   ├── service/CourseService.java         ← 목록/상세에서 PUBLISHED만
│   │   └── controller/CourseController.java
│   ├── category/
│   ├── enrollment/
│   ├── progress/
│   └── review/
└── global/
    ├── security/
    │   ├── SecurityConfig.java                ← 경로별 hasRole 추가
    │   ├── UserPrincipal.java                 ← getAuthorities()에 ROLE_ 반영
    │   ├── CustomUserDetailsService.java
    │   ├── OwnershipValidator.java            🆕 소유자 검증 유틸
    │   └── jwt/
    │       ├── JwtAuthFilter.java
    │       └── TokenProvider.java             ← role 클레임 추가 (선택)
    ├── error/
    │   ├── CustomException.java
    │   ├── ErrorCode.java
    │   ├── GlobalExceptionHandler.java
    │   └── InstructorErrorCode.java           🆕
    └── response/ApiResponse.java
```

### 3-2. 신규 패키지 요약

| 패키지 | 역할 |
|--------|------|
| `domain.instructor` | 강사 전용 API (강의 CRUD, 섹션/렉처 관리, 대시보드, 프로필 편집/공개 조회, 강의 취소) |
| `domain.cart` | 장바구니 (담기/조회/삭제) |
| `domain.order` | 주문 헤더·항목. 장바구니 스냅샷 → 주문 생성 |
| `domain.payment` | 모의 결제 & 환불. `MockPaymentGateway`가 토스 자리를 대신 (항상 SUCCESS) |
| `domain.playback` | 재생 위치 upsert, "이어듣기" 계산 |
| `domain.bookmark` | 렉처 북마크 CRUD |
| `global.security.OwnershipValidator` | 소유자 검증 공통 유틸. IDOR 방지 2차 방어선 (Course, Order, Bookmark, Playback 등 모두 사용) |

---

## 4. Frontend 디렉토리 구조

```
P2/frontend/src/
├── App.tsx                                    ← RoleGuard 라우트 추가
├── main.tsx
├── api/
│   ├── client.ts
│   ├── auth.ts
│   ├── courses.ts
│   ├── enrollments.ts
│   ├── reviews.ts
│   └── instructor.ts                          🆕 강사 API 클라이언트
├── context/
│   └── AuthContext.tsx                        ← role/userId 확장
├── components/
│   ├── ProtectedRoute.tsx                     (P1 그대로)
│   ├── RoleGuard.tsx                          🆕
│   ├── NavBar.tsx                             ← 강사 메뉴 조건부 렌더링
│   └── InstructorCard.tsx                     🆕 강의 상세/목록에 삽입
├── pages/
│   ├── LoginPage.tsx
│   ├── SignupPage.tsx                         ← role 선택 UI
│   ├── CoursesPage.tsx                        ← 강사 링크 + 가격 노출
│   ├── CourseDetailPage.tsx                   ← 강사 카드 + "장바구니 담기"
│   ├── CartPage.tsx                           🆕 /cart
│   ├── CheckoutPage.tsx                       🆕 /checkout (주문 확인 + 모의 결제 버튼)
│   ├── OrdersPage.tsx                         🆕 /orders
│   ├── OrderDetailPage.tsx                    🆕 /orders/:id
│   ├── MyCoursesPage.tsx                      ← 이어듣기 카드 추가
│   ├── MyBookmarksPage.tsx                    🆕 /bookmarks
│   ├── LearningPage.tsx                       ← 재생 위치 저장 + 북마크 UI
│   ├── InstructorPublicProfilePage.tsx        🆕 /instructors/:userId
│   └── instructor/                            🆕 강사 전용 페이지
│       ├── InstructorDashboardPage.tsx
│       ├── InstructorCourseListPage.tsx
│       ├── InstructorCourseEditorPage.tsx
│       ├── InstructorCourseStudentsPage.tsx
│       └── InstructorProfileEditPage.tsx
├── guards/
│   └── (RoleGuard는 components/로 편입)
└── hooks/
    └── useInstructorAuth.ts                   🆕 role 체크 훅
```

---

## 5. 시스템 아키텍처 다이어그램

### 5-1. 전체 레이어

```
┌────────────────────────────────────────────────────────────────┐
│                         Browser (React 18)                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Router  →  ProtectedRoute  →  RoleGuard  →  Page        │  │
│  │                              │                           │  │
│  │                         AuthContext                       │  │
│  │                  (userId, role, token)                    │  │
│  │                              │                           │  │
│  │                          Axios API                        │  │
│  └──────────────────────────────┬───────────────────────────┘  │
└─────────────────────────────────┼─────────────────────────────┘
                                  │ HTTP + Bearer JWT
                                  ▼
┌────────────────────────────────────────────────────────────────┐
│                  Spring Boot 4.0.2 (backend)                    │
│                                                                 │
│  ┌──────────────────── Spring Security Filter Chain ────────┐  │
│  │  1. CorsFilter                                            │  │
│  │  2. CsrfFilter (stateless → disabled)                     │  │
│  │  3. JwtAuthFilter  ──► UserPrincipal에 role 주입           │  │
│  │  4. UsernamePasswordAuthenticationFilter                  │  │
│  │  5. AuthorizationFilter                                   │  │
│  │       ↓ SecurityConfig의 URL 규칙 매칭                      │  │
│  │       ↓ @PreAuthorize 평가                                │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌────────────────────── Controller 레이어 ──────────────────┐  │
│  │  AuthController · CourseController                        │  │
│  │  InstructorCourseController · InstructorDashboardController│ │
│  │  InstructorProfileController · InstructorPublicController │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌────────────────────── Service 레이어 ─────────────────────┐  │
│  │  InstructorCourseService, CourseService, ...              │  │
│  │    ↳ OwnershipValidator (소유자 2차 검증)                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌─────────────────── Repository (Spring Data JPA) ─────────┐  │
│  │  UserRepository, CourseRepository, ...                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                  │
└─────────────────────────────┼──────────────────────────────────┘
                              ▼
                        ┌──────────┐
                        │  MySQL   │
                        └──────────┘
```

### 5-2. Spring Security 필터 체인 (P2)

```
Request
  │
  ▼
┌─────────────────────┐
│ CorsFilter          │  localhost:5173 허용
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ JwtAuthFilter       │  Authorization 헤더 파싱
│                     │  → TokenProvider.validate
│                     │  → UserPrincipal 생성 (role 포함 ← 🆕)
│                     │  → SecurityContext 저장
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ AuthorizationFilter │  SecurityConfig 규칙
│                     │  e.g. /api/instructor/** → hasRole("INSTRUCTOR")
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ @PreAuthorize       │  메서드 레벨 (선택적 2차 게이트)
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ Controller          │
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ Service             │  OwnershipValidator로 3차 검증
│                     │  (IDOR 방지)
└─────────┬───────────┘
          ▼
       Response
```

---

## 6. 권한 검사 흐름도 (강사 리소스)

```
PUT /api/instructor/courses/42   Authorization: Bearer <jwt>
  │
  ▼
┌───────────────────────────────────────┐
│ JwtAuthFilter                         │
│  - validate(token)                    │
│  - userId = 5, role = INSTRUCTOR      │
│  - UserPrincipal.authorities          │
│    = [ROLE_INSTRUCTOR]                │
└──────────────────┬────────────────────┘
                   ▼
┌───────────────────────────────────────┐
│ SecurityConfig 규칙                    │
│  /api/instructor/**                   │
│    .hasRole("INSTRUCTOR")  ✅         │
└──────────────────┬────────────────────┘
                   ▼
┌───────────────────────────────────────┐
│ @PreAuthorize("hasRole('INSTRUCTOR')")│
│   (컨트롤러 클래스 레벨)  ✅             │
└──────────────────┬────────────────────┘
                   ▼
┌───────────────────────────────────────┐
│ Controller.update(courseId, userId=5) │
│  → service.update(...)                │
└──────────────────┬────────────────────┘
                   ▼
┌───────────────────────────────────────┐
│ InstructorCourseService               │
│  Course c = ownership                 │
│    .requireOwnedCourse(42, 5)         │
│      ├─ course.instructor_id == 5 ?   │
│      ├─ YES → 통과                     │
│      └─ NO  → 404 COURSE_NOT_FOUND     │
└──────────────────┬────────────────────┘
                   ▼
              정상 업데이트
```

**3중 방어선:**

| 단계 | 검사 내용 | 실패 시 응답 |
|------|---------|-------------|
| 1. SecurityConfig URL 규칙 | role == INSTRUCTOR? | 403 |
| 2. `@PreAuthorize` | role == INSTRUCTOR? (중복 확인) | 403 |
| 3. `OwnershipValidator` | course.instructor_id == 요청자 userId? | **404** (info-leak 방지) |

> 왜 3단계인가: 한 단계가 누락돼도 다른 단계가 막아준다. 특히 3단계(소유자 검증)는 "내가 강사라도 **남의 강의**는 못 건드린다"를 보장하는 IDOR 방지 핵심.

---

## 7. 데이터 흐름 시나리오

### 7-1. 시나리오 A: 강사가 강의를 발행한다

```
[Frontend] InstructorCourseEditorPage                             [Backend]
  │
  │  1) 편집 화면에서 "발행" 버튼 클릭
  │     POST /api/instructor/courses/42/publish
  │     Authorization: Bearer <jwt>
  ├──────────────────────────────────────────────────►
  │                                                        JwtAuthFilter
  │                                                          ↓ userId=5
  │                                                          ↓ role=INSTRUCTOR
  │                                                        SecurityConfig
  │                                                          ↓ /instructor/** ✅
  │                                                        @PreAuthorize ✅
  │                                                        InstructorCourseController.publish
  │                                                          ↓
  │                                                        InstructorCourseService
  │                                                          ↓ OwnershipValidator
  │                                                          ↓ Course c = find(42)
  │                                                          ↓ c.instructor_id == 5 ✅
  │                                                          ↓ c.publish_status = DRAFT ✅
  │                                                          ↓ 섹션 ≥ 1, 렉처 ≥ 1 ✅
  │                                                          ↓ c.publish_status = PUBLISHED
  │                                                          ↓ c.published_at = now()
  │                                                          ↓ save
  │                                                        200 OK
  │  ◄─────────────────────────────────────────────────
  │  2) 토스트 "발행되었습니다" + 목록 갱신
  ▼
```

**실패 경로:**

| 단계 | 실패 | 응답 |
|------|-----|-----|
| 1 | 토큰 만료 | 401 |
| 2 | role == STUDENT | 403 |
| 3 | 남의 강의 | 404 |
| 4 | 이미 PUBLISHED | 409 `ALREADY_PUBLISHED` |
| 5 | 섹션 0개 | 422 `PUBLISH_VALIDATION_FAILED` |

### 7-1-A. 시나리오 A': 학생이 장바구니에 담고 모의 결제한다 🆕

```
[Frontend] CoursesPage/CartPage/CheckoutPage                    [Backend]
  │
  │  1) CourseDetailPage에서 "장바구니 담기"
  │     POST /api/cart/items { courseId: 1 }
  ├──────────────────────────────────────────────────►
  │                                                       CartController → CartService
  │                                                         ↓ Course PUBLISHED 확인
  │                                                         ↓ enrollments 중복 확인
  │                                                         ↓ cart_items upsert
  │  ◄─────────────────────────────────────────────────
  │  2) /cart 로 이동 → 목록 확인 → "결제하기"
  │     POST /api/orders   (본문 없음)
  ├──────────────────────────────────────────────────►
  │                                                       OrderService.createFromCart(userId)
  │                                                         ↓ cart 비었으면 400
  │                                                         ↓ order_no 생성 (ORD-YYYYMMDD-NNNN)
  │                                                         ↓ orders INSERT (status=PENDING)
  │                                                         ↓ 각 cart_item → order_items
  │                                                             (course_title_snapshot, price_snapshot 복사)
  │                                                         ↓ total_amount 합산
  │  ◄─────────────────────────────────────────────────
  │     { orderId: 77, status: PENDING }
  │
  │  3) /checkout/:orderId → 확인 → "결제 진행"
  │     POST /api/payments/checkout { orderId: 77 }
  ├──────────────────────────────────────────────────►
  │                                                       PaymentService.checkout(userId, orderId)
  │                                                         ↓ OwnershipValidator.requireOwnedOrder
  │                                                         ↓ status == PENDING 확인
  │                                                         ↓ MockPaymentGateway.process()
  │                                                             ├─ tx_id = UUID
  │                                                             └─ 항상 SUCCESS 반환
  │                                                         ↓ payments INSERT
  │                                                             (method=MOCK_CARD, status=SUCCESS)
  │                                                         ↓ orders.status=PAID, paid_at=now
  │                                                         ↓ order_items별 enrollments INSERT
  │                                                         ↓ cart 비우기 (delete user's cart_items)
  │                                                       [단일 @Transactional로 묶음]
  │  ◄─────────────────────────────────────────────────
  │     { status: PAID, enrollmentIds: [301, 302] }
  │
  │  4) "결제 완료" 토스트 → /my/courses 로 이동
  │     두 강의 카드 즉시 노출
  ▼
```

**실패 경로:**

| 단계 | 실패 | 응답 |
|------|-----|-----|
| 2 | cart 비었음 | 400 `EMPTY_CART` |
| 2 | DRAFT/ARCHIVED 강의가 담긴 상태 | 409 `COURSE_NOT_PURCHASABLE` |
| 3 | 남의 orderId | 404 |
| 3 | 이미 PAID | 409 `ORDER_NOT_PAYABLE` |

### 7-1-B. 시나리오 A'': 환불 🆕

```
[Frontend] OrderDetailPage                                       [Backend]
  │  "부분 환불" (orderItem 502만)
  │  POST /api/orders/77/refund { reason: "USER_REQUEST", orderItemIds:[502] }
  ├──────────────────────────────────────────────────►
  │                                                       RefundService.refund(...)
  │                                                         ↓ OwnershipValidator.requireOwnedOrder
  │                                                         ↓ order.status ∈ {PAID, PARTIAL_REFUNDED}
  │                                                         ↓ target order_items 검증
  │                                                         ↓ 각 item: status=REFUNDED, refunded_at=now
  │                                                         ↓ refunds INSERT (reason=USER_REQUEST)
  │                                                         ↓ 해당 강의 enrollments 삭제
  │                                                         ↓ orders.refunded_amount += 59000
  │                                                         ↓ 전부 REFUNDED면 orders.status=REFUNDED
  │                                                           일부면 PARTIAL_REFUNDED
  │                                                       [@Transactional]
  │  ◄─────────────────────────────────────────────────
  ▼
```

**강의 단체 취소 (강사 트리거):** `POST /api/instructor/courses/{id}/cancel` → 해당 강의를 포함한 모든 PAID order_items 조회 → 동일 로직을 `reason=COURSE_CANCELLED`로 반복 → 강의 `ARCHIVED`.

### 7-2. 시나리오 B: 학생이 수강신청 후 진도를 올린다

```
[Frontend] CourseDetailPage                                      [Backend]
  │
  │  1) 학생이 "수강신청" 클릭
  │     POST /api/enrollments { courseId: 42 }
  ├──────────────────────────────────────────────────►
  │                                                        JwtAuthFilter
  │                                                          ↓ userId=9 role=STUDENT
  │                                                        EnrollmentController
  │                                                          ↓ CourseService.findPublished(42)
  │                                                          ↓ publish_status == PUBLISHED ✅
  │                                                          ↓ 중복 enrollment 체크
  │                                                          ↓ Enrollment 생성
  │                                                        201 Created
  │  ◄─────────────────────────────────────────────────
  │     { enrollmentId: 77 }
  │
  │  2) /courses/42/learn/77 로 이동
  │     LearningPage 렌더 → 첫 렉처 선택
  │
  │  3) 영상 시청 완료 → "완료 처리"
  │     POST /api/progress/complete { enrollmentId:77, lectureId:301 }
  ├──────────────────────────────────────────────────►
  │                                                        ProgressController
  │                                                          ↓ Enrollment.owner_id == userId ✅
  │                                                          ↓ LectureProgress upsert
  │                                                        200 OK
  │  ◄─────────────────────────────────────────────────
  │
  │  4) GET /api/enrollments/77/progress-rate
  ├──────────────────────────────────────────────────►
  │                                                        { rate: 0.25 }
  │  ◄─────────────────────────────────────────────────
  │  5) 진도바 25% 갱신
  ▼
```

### 7-3. 시나리오 C: 이어듣기 + 북마크 🆕

```
[Frontend] LearningPage                                          [Backend]
  │  재생 중 10초마다 PUT /api/playback
  │     { enrollmentId: 77, lectureId: 301, positionSeconds: 437 }
  ├──────────────────────────────────────────────────►
  │                                                       PlaybackService.upsert
  │                                                         ↓ enrollment.ownedBy(userId)
  │                                                         ↓ playback_positions UPSERT
  │                                                         ↓ last_played_at = now
  │  ◄─────────────────────────────────────────────────
  │
  │  이탈 → 다음 접속 → MyCoursesPage "이어보기" 버튼 클릭
  │  GET /api/playback/enrollments/77/resume
  ├──────────────────────────────────────────────────►
  │                                                         ↓ last_played_at DESC LIMIT 1
  │  ◄─────────────────────────────────────────────────
  │     { lecture:{...}, positionSeconds: 437 }
  │
  │  LearningPage 열림 → <video currentTime={437}>
  │
  │  학생이 "현재 위치 북마크" 클릭
  │  POST /api/bookmarks { lectureId:301, positionSeconds:437, memo:"면접 단골" }
  ├──────────────────────────────────────────────────►
  │                                                       BookmarkService.create
  │                                                         ↓ NOT_ENROLLED 체크
  │                                                         ↓ bookmarks INSERT
  │  ◄─────────────────────────────────────────────────
  ▼
```

---

## 8. 프론트 보호 라우트 전략

### 8-1. 계층 구조

```
<Route element={<ProtectedRoute />}>
  {/* 로그인 필요 */}
  <Route path="/my/courses" element={<MyCoursesPage />} />
  <Route path="/cart" element={<CartPage />} />                          {/* 🆕 */}
  <Route path="/checkout/:orderId" element={<CheckoutPage />} />         {/* 🆕 */}
  <Route path="/orders" element={<OrdersPage />} />                      {/* 🆕 */}
  <Route path="/orders/:id" element={<OrderDetailPage />} />             {/* 🆕 */}
  <Route path="/bookmarks" element={<MyBookmarksPage />} />              {/* 🆕 */}

  <Route element={<RoleGuard allow="INSTRUCTOR" />}>
    {/* 로그인 + 강사 전용 */}
    <Route path="/instructor/dashboard" element={<InstructorDashboardPage />} />
    <Route path="/instructor/courses" element={<InstructorCourseListPage />} />
    <Route path="/instructor/courses/new" element={<InstructorCourseEditorPage />} />
    <Route path="/instructor/courses/:id/edit" element={<InstructorCourseEditorPage />} />
    <Route path="/instructor/courses/:id/students" element={<InstructorCourseStudentsPage />} />
    <Route path="/instructor/profile" element={<InstructorProfileEditPage />} />
  </Route>
</Route>

{/* 공개 라우트 */}
<Route path="/" element={<Navigate to="/courses" />} />
<Route path="/login" element={<LoginPage />} />
<Route path="/signup" element={<SignupPage />} />
<Route path="/courses" element={<CoursesPage />} />
<Route path="/courses/:id" element={<CourseDetailPage />} />
<Route path="/instructors/:userId" element={<InstructorPublicProfilePage />} />
```

### 8-2. 가드별 책임

| 가드 | 검사 | 실패 동작 |
|------|-----|---------|
| `ProtectedRoute` | `AuthContext.isAuthenticated` | `/login`으로 redirect |
| `RoleGuard allow="INSTRUCTOR"` | `AuthContext.role === 'INSTRUCTOR'` | `/courses`로 redirect |

### 8-3. UX 원칙

- **로그아웃 상태**에서 `/instructor/*` 진입 → `/login`
- **STUDENT 계정**으로 `/instructor/*` 진입 → `/courses` (조용히 리다이렉트, 에러 토스트 없음)
- **INSTRUCTOR 계정**이 `/courses`, `/my/courses`, `/instructor/*` 자유 왕복 가능 (강사도 학생처럼 수강 가능)
- 네비게이션 바는 `role` 기반 조건부 렌더링으로 **클릭 전에** 접근 불가 메뉴를 숨긴다

---

## 9. 환경 변수

| 키 | 용도 | P2 변경 |
|----|-----|--------|
| `JWT_SECRET_KEY` | JWT 서명 키 | 동일 |
| `SPRING_DATASOURCE_URL` | MySQL 연결 | 동일 (DB명만 p2로 분리 권장) |
| `SPRING_DATASOURCE_USERNAME` | | 동일 |
| `SPRING_DATASOURCE_PASSWORD` | | 동일 |
| `VITE_API_BASE_URL` | 프론트 → 백엔드 | 동일 |

권장: P2에서는 `devlearn_p2` DB 스키마를 별도로 두어 P1과 격리.

---

## 10. 관련 문서

- 데이터 모델: [02-data-model.md](./02-data-model.md)
- API 설계: [03-api-design.md](./03-api-design.md)
- 보안 설계: [09-security-design.md](./09-security-design.md)
- 강사 API 심화: [07-instructor-api-spec.md](./07-instructor-api-spec.md)
