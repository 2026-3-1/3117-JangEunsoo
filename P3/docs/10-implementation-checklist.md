# 10. 구현 체크리스트 (Phase별 DoD)

P1의 모든 기능이 동작하는 상태에서 출발한다고 가정. 각 Phase를 순서대로 진행하고, DoD를 다음 Phase로 넘기기 전에 모두 통과시켜야 한다.

> **PR 단위 원칙**: 1개 Phase = 1개 PR. PR 제목·커밋 메시지·문서는 한국어.

---

## Phase 1. DB 마이그레이션 & `User.role`

- [ ] `Role` enum 추가: `STUDENT`, `INSTRUCTOR`
- [ ] `User`에 `@Enumerated(EnumType.STRING) Role role` 필드 추가 (default `STUDENT`)
- [ ] `schema-p2-migration.sql` 작성 (users.role 백필, courses.instructor_id 백필+NOT NULL, 인덱스)
- [ ] `Course`에 `instructor_id`, `publish_status`, `published_at`, `price` 필드 추가
- [ ] `PublishStatus` enum: `DRAFT`/`PUBLISHED`/`ARCHIVED`
- [ ] `InstructorProfile` 엔티티 + Repository
- [ ] **DoD**: 앱 부팅 시 신규 테이블 생성, 마이그레이션 SQL 적용 후 P1 데이터가 깨지지 않음

## Phase 2. Spring Security 역할 인가

- [ ] `JwtAuthenticationFilter`가 토큰에서 `role` claim 파싱 → `ROLE_<...>` 권한 주입
- [ ] `TokenProvider.createAccessToken`에 `role` claim 포함
- [ ] `SecurityConfig`의 URL 규칙 갱신 (05-security-design.md §3 참고)
- [ ] `@EnableMethodSecurity(prePostEnabled=true)`
- [ ] `OwnershipValidator` 작성 (course, order, enrollment, bookmark용 메서드)
- [ ] **DoD**: STUDENT 토큰으로 `/api/instructor/courses` POST → `403`. 타인 강의 PUT → `404`

## Phase 3. 강사 강의 CRUD API

- [ ] `InstructorCourseController` (@ `/api/instructor/courses`, `@PreAuthorize("hasRole('INSTRUCTOR')")`)
- [ ] CRUD + publish + archive + cancel 엔드포인트
- [ ] `InstructorSectionController`, `InstructorLectureController`
- [ ] 발행 검증 (PUBLISH_VALIDATION_FAILED 조건)
- [ ] 폐강 트랜잭션 (06-business-rules.md §1 참고)
- [ ] `CourseRepository.findWithFilters`는 `publish_status='PUBLISHED'`만 반환하도록 수정 (학생 공개)
- [ ] **DoD**: 단위 테스트로 발행 조건 미달·소유자 검증·폐강 환불 흐름 통과

## Phase 4. 강사 대시보드 & 프로필 API

- [ ] `InstructorProfileController` GET/PUT
- [ ] `InstructorPublicController` (`/api/instructors/{userId}`) — 공개
- [ ] `InstructorDashboardController` — 통계 집계
- [ ] **DoD**: Swagger에서 응답 스키마 확인 가능

## Phase 5. 프론트엔드 공통 기반

- [ ] `AuthContext`에 `role` 상태 추가
- [ ] `fetchMe()` 호출 후 `userId`/`role`/`username` 설정
- [ ] `RoleGuard` 컴포넌트
- [ ] `NavBar` role 조건부 메뉴
- [ ] axios 인터셉터 (Authorization 자동 부착, 401 → refresh 재시도)
- [ ] **DoD**: 새로고침 후에도 role 유지, 잘못된 역할로 강사 페이지 접근 시 `/courses` 리다이렉트

## Phase 6. 강사 콘솔 UI

- [ ] `InstructorDashboardPage`
- [ ] `InstructorCourseListPage` (탭별 필터)
- [ ] `InstructorCourseEditorPage` (생성/수정 공용)
- [ ] `InstructorCourseStudentsPage`
- [ ] `InstructorProfileEditPage`
- [ ] **DoD**: 강사 계정으로 강의 생성 → 섹션·강의차시 추가 → 발행 → `/courses`에서 노출

## Phase 7. 공개 프로필 & 학생 UI 보강

- [ ] `InstructorPublicProfilePage`
- [ ] `CourseDetailPage`에 강사 카드(`InstructorCard`) → 클릭 시 공개 프로필로 이동
- [ ] **DoD**: 비로그인 사용자도 `/instructors/{userId}` 접근 가능

## Phase 8. 장바구니·주문·모의 결제 (Backend)

- [ ] `CartItem` 엔티티 + Repository + Controller + Service
- [ ] `Order`, `OrderItem` 엔티티 + 상태머신 메서드 (`markPaid`, `applyRefund` 등)
- [ ] `OrderService.createFromCart(userId)` — 스냅샷 생성 + cart 비우지 않음(결제 후 비움)
- [ ] `Payment`, `Refund` 엔티티
- [ ] `MockPaymentGateway` (인터페이스 + 구현)
- [ ] `PaymentController` checkout + refund
- [ ] `EnrollmentService`에 `COURSE_NOT_FREE` 가드 추가
- [ ] **DoD**:
  - 결제 SUCCESS 시 enrollment 자동 생성 (단위 테스트 `CheckoutTest` 통과)
  - 진행 중 주문 재결제 → `409 ORDER_NOT_PAYABLE`
  - 유료 강의에 `/api/enrollments` → `400 COURSE_NOT_FREE` (`EnrollmentPaidGuardTest`)

## Phase 9. 장바구니·결제·주문 내역 (Frontend)

- [ ] `CartPage`, `CheckoutPage`, `OrdersPage`, `OrderDetailPage`
- [ ] `cart.ts`, `order.ts`, `payment.ts` API 모듈
- [ ] CourseDetailPage 분기 (무료/유료, 수강 여부)
- [ ] 환불 UI (전체/일부)
- [ ] **DoD**: 학생이 유료 강의를 장바구니→결제→내 수강에서 즉시 시청 가능. 환불 후 내 수강에서 사라짐

## Phase 10. 이어듣기·북마크·리뷰 진도 게이트

- [ ] `PlaybackPosition` 엔티티 + 업서트 Service
- [ ] `Bookmark` 엔티티 + CRUD
- [ ] `ReviewService.create()`에 진도 80% 게이트 (서버 재계산)
- [ ] `ReviewProgressGateException` + GlobalExceptionHandler에 body 매핑
- [ ] `LearningPage` 비디오 위치 복원/주기 저장
- [ ] `MyBookmarksPage`
- [ ] **DoD**:
  - 진도 79% 상태에서 리뷰 작성 → `422 REVIEW_PROGRESS_GATE` + body에 `currentProgressRate`/`requiredRate`
  - 페이지 새로고침 후 비디오가 마지막 위치에서 재생
  - `ReviewProgressGateTest` 통과

## Phase 11. E2E·통합·문서

- [ ] `AuthorizationGateTest` — 모든 URL 규칙 통과 매트릭스
- [ ] `OwnershipValidatorTest` — IDOR 시 404 확인
- [ ] `CheckoutTest`, `EnrollmentPaidGuardTest`, `ReviewProgressGateTest`
- [ ] `DevLearnApplicationTests` 스모크 테스트
- [ ] CORS 환경변수 외부화 (`CORS_ALLOWED_ORIGINS`)
- [ ] Docker Compose 동작 확인 (mysql healthcheck → backend → frontend)
- [ ] README 업데이트 (실행 절차)
- [ ] **DoD**: `./gradlew test` 전체 통과, `docker compose up -d` 만으로 정상 기동

---

## 회귀 테스트 시나리오 (수동)

각 Phase 완료 후 다음을 처음부터 끝까지 한 번씩 돌려본다:

### 시나리오 A. 학생 무료 수강
1. alice로 로그인
2. `/courses` → 무료 강의(Spring Boot 입문) 선택
3. "수강 신청" → enrollment 생성 → "강의실로"
4. 첫 강의차시 시청 중 → 50초 즈음 페이지 새로고침 → 50초 부근에서 재생
5. 모든 강의차시 완료 → `/courses/1` 로 돌아와 리뷰 작성 → 성공

### 시나리오 B. 학생 유료 결제 → 환불
1. alice로 로그인
2. `/courses/2` (JPA로 배우는 DB 설계, 49,000원) → 장바구니 담기
3. `/courses/3` (React 19 실전, 39,000원) → 장바구니 담기
4. `/cart` → 주문하기 → `/checkout/:orderId`
5. 결제하기 → `/orders/:orderId` 또는 `/my/courses`로 이동, enrollment 2건 생성됨
6. `/orders/:orderId` → React 19만 환불 → 주문 상태 `PARTIAL_REFUNDED`, `/my/courses`에서 React 19 사라짐

### 시나리오 C. 강사 발행 → 폐강
1. tom으로 로그인 → `/instructor/courses/new` → DRAFT로 생성
2. 섹션 1개, 강의차시 1개 추가 → 발행 → `/courses`에서 노출
3. alice로 로그인 → 해당 강의 결제 → 수강
4. tom으로 다시 로그인 → 해당 강의 폐강
5. alice 계정의 `/my/courses`에서 사라짐, `/orders/:id`는 REFUNDED 상태

### 시나리오 D. 진도 게이트
1. alice로 로그인 → 50% 진도 상태에서 리뷰 작성 시도 → `422 REVIEW_PROGRESS_GATE` + 메시지
2. 80% 채운 후 → 리뷰 작성 성공

### 시나리오 E. IDOR 방지
1. alice로 로그인 후 다른 사용자의 orderId로 `GET /api/orders/{id}` 호출 → `404 ORDER_NOT_FOUND`
2. STUDENT 토큰으로 `POST /api/instructor/courses` → `403 ACCESS_DENIED`
3. tom으로 로그인 후 jane의 강의 id로 `PUT /api/instructor/courses/{id}` → `404 COURSE_NOT_FOUND`

---

## 작업 우선순위 팁

1. **Backend 먼저, Frontend는 나중에**. Swagger로 동작 확인하면서 진행
2. **각 Phase가 PR**, 머지 후 다음 Phase. 한 번에 다 묶지 말 것
3. **테스트는 회귀 보호용으로 최소 5개 (위에 명시한 것)**. 더 많이 작성하기보다 정확히 작성
4. **하위호환 강박**: P1 경로/응답은 절대 변경하지 않는다. 추가만
5. **에러 메시지·UI 텍스트는 한국어**. 코드/식별자는 영어
