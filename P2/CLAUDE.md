# CLAUDE.md — DevLearn P2

Claude Code 작업 컨텍스트. P2의 두 축:
1. **역할 기반 접근제어** — INSTRUCTOR(강사) vs STUDENT, 강사용 강의 CRUD·대시보드·프로필
2. **학생 수강 라이프사이클 확장** — 장바구니 · 🆕 **모의 결제** · 환불 · 이어듣기 · 북마크 · 리뷰 80% 게이트

JWT 인증은 P1에서 이미 구현됨. P2 결제는 **실 PG 연동 없음** — `MockPaymentGateway`가 항상 SUCCESS 반환, `orders`·`payments` 레코드만 남김.

## Project

**devlearn P2** — 2026년 3학년 1학기 프로젝트 실습 수업의 인강사이트. P1(JWT 인증·강의 목록·수강·진도·리뷰)을 기반으로 **강사 역할 도입** + **수강 플로우(장바구니→모의 결제→환불)·이어듣기·북마크·리뷰 진도 게이트**를 추가한다. 학생과 강사가 공존하는 커뮤니티형 플랫폼으로 확장.

- 전체 PRD: [docs/](./docs/) (9개 문서)
- 참고 스타일: `../P1/tmp/docs/` (Coding Voca PRD)

---

## Commands

### Backend

```bash
cd P2/backend

./gradlew bootRun                 # 개발 실행
./gradlew build                   # 빌드
./gradlew test                    # 테스트
./gradlew clean build -x test     # 테스트 스킵 빌드
```

환경 변수 (`.env` 또는 IDE Run config):

```
JWT_SECRET_KEY=<base64_256bit_이상>
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/devlearn_p2
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=...
```

### Frontend

```bash
cd P2/frontend

npm install
npm run dev              # 개발 서버 (Vite, 기본 5173)
npm run build            # 프로덕션 빌드
npm run type-check       # tsc --noEmit
```

### DB

```bash
# 신규 스키마 생성 (최초 1회)
mysql -u root -p -e "CREATE DATABASE devlearn_p2 CHARACTER SET utf8mb4;"

# 앱 최초 기동 후 (ddl-auto=update가 테이블 생성 → 수동 마이그레이션 SQL 적용)
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/schema-p2-migration.sql
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/seed.sql
```

---

## Architecture

### 모노레포

```
P2/
├── backend/    # Spring Boot (P1 복사 후 P2 변경 반영)
├── frontend/   # React + Vite (P1 복사 후 P2 변경 반영)
├── docs/       # PRD 문서 세트 (9개)
└── CLAUDE.md   # 본 파일
```

P1은 읽기 전용 참고. 코드 수정은 **P2 트리에서만**.

### Backend 레이어

```
Request → CorsFilter → JwtAuthFilter(role 주입) → AuthorizationFilter
       → @PreAuthorize(class-level) → Controller
       → Service + OwnershipValidator(소유자 검증)
       → Repository(JPA) → MySQL
```

**3중 방어선**:
1. `SecurityConfig` URL 규칙 (`/api/instructor/**` → `hasRole("INSTRUCTOR")`)
2. Controller `@PreAuthorize("hasRole('INSTRUCTOR')")`
3. Service의 `OwnershipValidator.requireOwnedCourse(courseId, userId)` → 소유자 아니면 **404** (info-leak 방지)

### Backend 주요 패키지

```
com.jes.devlearn/
├── domain/
│   ├── user/          (User, Role enum)
│   ├── instructor/    🆕 (InstructorProfile, 강사 전용 컨트롤러/서비스)
│   ├── course/        (Course + price + PublishStatus, Section, Lecture)
│   ├── category/
│   ├── enrollment/    (무료는 직접 생성, 유료는 결제 후 자동 생성)
│   ├── progress/
│   ├── review/        (80% 진도 게이트)
│   ├── cart/          🆕 (CartItem)
│   ├── order/         🆕 (Order, OrderItem — price_snapshot, 상태머신)
│   ├── payment/       🆕 (Payment, MockPaymentGateway, Refund)
│   ├── playback/      🆕 (PlaybackPosition — UNIQUE(enrollment, lecture))
│   └── bookmark/      🆕 (Bookmark)
└── global/
    ├── security/      (SecurityConfig, UserPrincipal, OwnershipValidator, jwt/)
    ├── error/         (CustomException, ErrorCode, Order/Payment/Review 에러코드)
    └── response/      (ApiResponse)
```

### Frontend 구조

```
src/
├── App.tsx                                  # 라우팅 (ProtectedRoute + RoleGuard)
├── context/AuthContext.tsx                  # userId/role 확장
├── components/
│   ├── ProtectedRoute.tsx
│   ├── RoleGuard.tsx                        🆕
│   ├── NavBar.tsx                           (role 조건부 렌더)
│   └── InstructorCard.tsx                   🆕
├── pages/
│   ├── (P1 페이지들)
│   ├── InstructorPublicProfilePage.tsx      🆕  /instructors/:userId
│   ├── CartPage.tsx                         🆕  /cart
│   ├── CheckoutPage.tsx                     🆕  /checkout/:orderId
│   ├── OrdersPage.tsx                       🆕  /orders
│   ├── OrderDetailPage.tsx                  🆕  /orders/:orderId
│   ├── MyBookmarksPage.tsx                  🆕  /my/bookmarks
│   └── instructor/                          🆕
│       ├── InstructorDashboardPage.tsx
│       ├── InstructorCourseListPage.tsx
│       ├── InstructorCourseEditorPage.tsx
│       ├── InstructorCourseStudentsPage.tsx
│       └── InstructorProfileEditPage.tsx
└── api/
    ├── auth.ts  (fetchMe 추가)
    ├── cart.ts       🆕
    ├── order.ts      🆕
    ├── payment.ts    🆕
    ├── playback.ts   🆕
    ├── bookmark.ts   🆕
    └── instructor.ts 🆕
```

### 주요 엔티티 관계 (P2)

```
User ──1:1── InstructorProfile       (role=INSTRUCTOR 만)
  │
  │ 1:N (instructor_id)
  ▼
Course ─── PublishStatus (DRAFT | PUBLISHED | ARCHIVED)
  │       ─── price BIGINT (0 = 무료)
  │
  ├── 1:N ── Section ── 1:N ── Lecture (duration_seconds)
  │
  ├── 1:N ── Enrollment ── 1:N ── LectureProgress
  │              │        ── 1:N ── PlaybackPosition  🆕 UNIQUE(enrollment, lecture)
  │              │
  │              owner: User(역할 무관 수강 가능)
  │
  └── 1:N ── Review (80% 진도 게이트)

User 🆕 수강 확장
  │
  ├── 1:N ── CartItem          UNIQUE(user, course)
  │
  ├── 1:N ── Order ── 1:N ── OrderItem (price_snapshot, course_title_snapshot)
  │            │              │
  │            │              └── 결제 성공 시 enrollment 자동 생성
  │            ├── 1:1 ── Payment (status=SUCCESS, method=MOCK_CARD)
  │            └── 1:N ── Refund (reason: USER_REQUEST | COURSE_CANCELLED | ...)
  │
  └── 1:N ── Bookmark
```

**Order 상태머신:**
```
PENDING → PAID → (REFUNDED | PARTIAL_REFUNDED)
   └──→ CANCELLED
```
- `PENDING` 아닐 때 `checkout` 호출 시 `409 ORDER_NOT_PAYABLE`
- 환불은 트랜잭션 내에서 enrollment hard delete 포함

---

## Implementation Status

**현재**: PRD 작성 완료, 구현 미시작 (2026-04-20 기준)

| Phase | 이름 | 상태 |
|-------|------|-----|
| 1 | DB 마이그레이션 & User.role | TODO |
| 2 | Spring Security 역할 인가 | TODO |
| 3 | 강사 강의 CRUD API | TODO |
| 4 | 강사 대시보드 & 프로필 API | TODO |
| 5 | 프론트 공통 기반 | TODO |
| 6 | 강사 콘솔 UI | TODO |
| 7 | 공개 프로필 & 학생 UI | TODO |
| 🆕 8 | 장바구니·주문·모의 결제 (백엔드) | TODO |
| 🆕 9 | 장바구니·결제·주문 내역 (프론트) | TODO |
| 🆕 10 | 이어듣기·북마크·리뷰 진도 게이트 | TODO |
| 11 | E2E 테스트 & 통합 (기존 Phase 8) | TODO |

자세한 DoD는 [docs/06-implementation-checklist.md](./docs/06-implementation-checklist.md).

---

## API Error Format

P1과 동일한 포맷 유지:

```json
{
  "success": false,
  "status": 403,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "해당 리소스에 접근할 권한이 없습니다."
  }
}
```

성공 응답:

```json
{
  "success": true,
  "status": 200,
  "data": { ... }
}
```

주요 P2 에러 코드:
- 강사/권한: `COURSE_NOT_FOUND`(404, 소유자 아닐 때도 사용), `ACCESS_DENIED`(403), `PUBLISH_VALIDATION_FAILED`(422), `ALREADY_PUBLISHED`(409)
- 🆕 수강 확장: `COURSE_NOT_FREE`(400), `CART_DUPLICATE`(409), `ALREADY_ENROLLED`(409), `ORDER_NOT_FOUND`(404, 타인 주문 포함), `ORDER_NOT_PAYABLE`(409), `CART_SNAPSHOT_INVALID`(422), `ENROLLMENT_NOT_FOUND`(404, 재생/진도 IDOR), `REVIEW_PROGRESS_GATE`(422, body에 `currentProgressRate`/`requiredRate`)

---

## P1 → P2 차이점

| 영역 | P1 | P2 |
|------|-----|-----|
| 사용자 역할 | 없음 (모두 동일) | STUDENT / INSTRUCTOR |
| JWT 클레임 | sub=userId | sub=userId + role (권장) |
| 강의 생성 | `POST /api/courses` (아무나) | `POST /api/instructor/courses` (INSTRUCTOR만) |
| 강의 소유 | `instructor_name` 문자열 | `instructor_id` FK + (backcompat용) `instructor_name` |
| 발행 상태 | 전부 공개 | `DRAFT`/`PUBLISHED`/`ARCHIVED` |
| 목록 필터 | 전체 반환 | `PUBLISHED`만 반환 |
| 강사 프로필 | 없음 | `instructor_profiles` 테이블 + `/api/instructors/:userId` |
| 라우팅 가드 | `ProtectedRoute`만 | `+RoleGuard` |
| 🆕 강의 가격 | 필드 없음 | `courses.price` BIGINT (0 = 무료) |
| 🆕 유료 수강 | 없음 | 장바구니→주문→모의 결제→enrollment 자동 생성 |
| 🆕 무료 수강 | `POST /api/enrollments` | 동일. 단 유료에 호출하면 `400 COURSE_NOT_FREE` |
| 🆕 리뷰 게이트 | 제한 없음 | 진도율 ≥ 80% 필수 (서버 재계산) |
| 🆕 재생 위치 | 없음 | `PUT /api/playback` 주기 업서트, 이어듣기 |
| 🆕 북마크 | 없음 | `/my/bookmarks` |
| 🆕 강사 폐강 | 없음 | `POST /api/instructor/courses/{id}/cancel` — 일괄 환불 + ARCHIVED |

---

## Key Relationships

- `User.role` (enum STUDENT | INSTRUCTOR) — default STUDENT
- `User` 1:1 `InstructorProfile` — role=INSTRUCTOR 인 유저만
- `Course.instructor_id` → `User.id` (FK)
- `Course.publish_status` (enum DRAFT | PUBLISHED | ARCHIVED)
- `Course.price` BIGINT (0 = 무료, 원 단위)
- 학생/강사 모두 `Enrollment`·`Review` 작성 가능 (역할 무관)
- **소유자 검증**: `course.instructor_id == currentUserId` 를 Service 계층에서 반드시 확인
- 🆕 `Order.user_id` → `User.id` — 주문/환불/북마크/재생 위치 조회 시 `userId = principal` 강제
- 🆕 `OrderItem.price_snapshot` — 주문 시점 가격 고정 (이후 강의 가격 변경 영향 없음)
- 🆕 `PlaybackPosition` UNIQUE(`enrollment_id`, `lecture_id`) — 업서트 기반
- 🆕 `CartItem` UNIQUE(`user_id`, `course_id`) — 중복 담기 409

---

## 코드 작업 시 주의

1. **새 컨트롤러 만들 때**: 강사 전용이면 **반드시** 클래스 레벨 `@PreAuthorize("hasRole('INSTRUCTOR')")` + Service에서 `OwnershipValidator` 호출
2. **DRAFT 은폐**: `CourseRepository`에서 학생 공개 조회는 `publish_status = 'PUBLISHED'` 필터 필수
3. **하위호환**: 기존 `/api/courses`, `/api/enrollments` 등 P1 경로·응답 변경 금지. 필드 **추가만**
4. **에러 코드**: 다른 유저·강사의 리소스 접근 시 **403이 아닌 404** (존재 은폐). 주문/북마크/재생 위치 모두 동일
5. **P1 건드리지 말 것**: `../P1/` 아래 코드는 읽기만
6. **한국어**: 커밋 메시지·문서·에러 메시지는 한국어 (P1 컨벤션 유지)
7. 🆕 **결제 금액은 서버에서만**: `POST /api/payments/checkout` body는 `{ orderId }` 하나. amount·price는 클라이언트에서 받지 않고 `orders.total_amount`·`order_items.price_snapshot`으로 재계산
8. 🆕 **주문 상태머신 enforcement**: `PENDING → PAID`만 허용. 이외 상태에서 checkout은 `409 ORDER_NOT_PAYABLE`. 트랜잭션 내 처리
9. 🆕 **리뷰 진도 게이트**: 서버가 항상 `progressRate` 재계산. 클라이언트가 보낸 값은 신뢰 금지. 미달 시 `422 REVIEW_PROGRESS_GATE` + body에 현재 진도율·기준(80)
10. 🆕 **유료 강의의 직접 enrollment 금지**: `POST /api/enrollments`에서 `course.price > 0`이면 `400 COURSE_NOT_FREE`. 유료는 반드시 order 경로

---

## 문서 인덱스

- [01-project-overview.md](./docs/01-project-overview.md) — 비전, 대상 사용자, 핵심 기능
- [02-data-model.md](./docs/02-data-model.md) — ERD, SQL, 마이그레이션
- [03-api-design.md](./docs/03-api-design.md) — 전체 엔드포인트 목록
- [04-architecture.md](./docs/04-architecture.md) — 기술 스택, 레이어, 흐름도
- [05-sample-data.md](./docs/05-sample-data.md) — seed 계정·강의
- [06-implementation-checklist.md](./docs/06-implementation-checklist.md) — Phase별 DoD
- [07-instructor-api-spec.md](./docs/07-instructor-api-spec.md) — 강사 API 심화
- [08-student-feature-spec.md](./docs/08-student-feature-spec.md) — 학생 측 변경점
- [09-security-design.md](./docs/09-security-design.md) — STRIDE, 권한 매트릭스
