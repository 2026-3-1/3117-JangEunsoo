# 01. 프로젝트 개요 — DevLearn P2

## 1. 비전

**DevLearn P2**는 P1의 "수강생 전용 학습 플랫폼"을 확장하여, **강의를 직접 만드는 강사(Instructor)** 와 **수강하는 학생(Student)** 이 공존하는 역할 기반 인강 플랫폼을 구축한다.

P1까지는 모든 가입자가 동일한 권한으로 강의를 조회/수강만 할 수 있었다면, P2는 다음을 추가한다.

- **역할(Role) 개념**: 회원가입 시 STUDENT / INSTRUCTOR 선택
- **강사 전용 기능**: 강의·섹션·렉처 CRUD, 발행(publish) 상태 관리, 수강생 목록·진도 조회, 대시보드
- **권한 기반 접근제어**: Spring Security `@PreAuthorize` + 소유자 검증으로 타 강사의 리소스 수정 차단
- **강사 프로필**: 공개 프로필 페이지, 학생이 강의 카드/상세에서 강사 정보 확인
- **수강생 확장 기능** 🆕
  - **장바구니**: 강의를 담아두고 한꺼번에 결제
  - **(모의) 결제 & 거래내역**: 토스 등 PG 연동 없이 버튼 클릭만으로 결제 성사/취소/환불 처리, 모든 거래 기록을 DB에 축적
  - **리뷰 작성 게이트**: 강의 진도 **80% 이상** 수강한 학생만 리뷰 작성 가능
  - **이어듣기**: 영상 재생 위치를 주기적으로 저장 → 다음 접속 시 해당 위치부터 자동 재개
  - **북마크**: 특정 렉처/재생 구간에 메모와 함께 책갈피 저장·조회

> **주의:** PDF 수업 계획상 P2에 예정되어 있던 JWT 인증은 **P1에서 이미 구현 완료** (커밋 `22b5e7a fix: 회원가입 후 자동 로그인 처리`). 따라서 P2의 실질 작업은 **"인증 위에 역할(Role) 체계 얹기 + 수강생 기능 확장"** 이다.
>
> **결제는 "모의(mock)"** 처리한다. 토스/KG이니시스 등 실제 PG 연동 없이, 결제 버튼 클릭 시 DB에 `orders`·`payments`·`refunds` 레코드만 남기고 성공/실패를 시뮬레이션한다. "토스는 결제만 성사시켜줄 뿐 모든 거래 내역은 우리쪽 DB에 남긴다"는 PDF 요구사항의 범위 내에서, 본 과제는 **DB 기록 책임**에 집중한다.

---

## 2. 핵심 가치

| # | 가치 | 설명 |
|---|------|------|
| 1 | **역할 분리** | 한 플랫폼 안에서 강사/학생이 각자의 UX와 권한으로 행동. 회원가입 시 역할 선택 또는 이후 승급 |
| 2 | **권한 기반 안전성** | 모든 강사 리소스 변경 요청은 JWT 인증 + `@PreAuthorize('INSTRUCTOR')` + **소유자 검증(IDOR 방지)** 3단 체크 |
| 3 | **강사 UX** | 강의 생성부터 섹션·렉처 편집, 발행, 수강생 확인까지 강사 전용 콘솔로 단일 창에서 처리 |
| 4 | **감사 가능성** | 누가 언제 어떤 강의를 만들고 수정했는지 FK와 타임스탬프로 추적 가능 (현재 `instructor_name` 문자열 → `instructor_id` FK 전환) |

---

## 3. 대상 사용자

| 역할 | 설명 | 핵심 행동 |
|------|------|----------|
| **STUDENT** (수강생) | P1에서 이어지는 기본 사용자. 강의 탐색·수강·리뷰 | 가입 → 로그인 → 강의 탐색 → 수강신청 → 학습(진도 기록) → 리뷰 작성 |
| **INSTRUCTOR** (강사, =선생님/관리자 역할) | P2에서 신규. 강의 공급자 | 가입 → 강사 승급 or 가입 시 선택 → 강의 생성 → 섹션/렉처 등록 → 발행 → 수강생·리뷰 모니터링 |

**미포함 (P3 이후):**
- 판매자(Seller) 분리, 결제/정산 역할 — P2에서는 강사가 곧 "관리자" 역할 겸임
- 플랫폼 운영 관리자(Admin) — P3 이후 도입 검토

---

## 4. 핵심 기능 (역할별)

### 4-1. 학생 (Student) 기능

| 카테고리 | 기능 | P1 대비 |
|----------|------|---------|
| 탐색 | 강의 목록/검색/필터(카테고리·난이도·키워드) | 가격 필드 노출 추가 |
| 탐색 | 강의 상세 조회 (섹션·렉처 목록 포함) | 강사 이름 → **강사 프로필 링크**로 확장, 가격 표시 |
| 탐색 | **강사 프로필 페이지** (`/instructors/:id`) | 🆕 신규 |
| 장바구니 | 담기 / 조회 / 개별 삭제 / 비우기 🆕 | 🆕 신규 (`/api/cart`) |
| 결제 | 장바구니 한꺼번에 결제 (모의) → 주문 생성 → 자동 수강 등록 🆕 | 🆕 신규 (`/api/orders`, `/api/payments/checkout`) |
| 주문 | 내 주문 내역 / 상세 조회 🆕 | 🆕 신규 (`/api/orders`, `/api/orders/{id}`) |
| 환불 | 주문 취소 요청 → 환불 처리 (모의) 🆕 | 🆕 신규 (`/api/orders/{id}/refund`) |
| 수강 | 결제 완료 시 자동 수강 등록 | P1의 수동 `POST /api/enrollments`는 유지하되 **결제 플로우가 메인** |
| 수강 | 내 수강 목록 / 수강 취소 | 유지 (단 결제건은 환불 플로우로 연동) |
| 학습 | 렉처 완료 처리, 진도율 조회 | 그대로 유지 |
| 학습 | **재생 위치 저장 / 이어듣기** 🆕 | 🆕 신규 (`/api/playback`) |
| 학습 | **북마크 추가 / 조회 / 삭제** 🆕 | 🆕 신규 (`/api/bookmarks`) |
| 리뷰 | 리뷰 작성 — **진도 80% 이상 게이트** 🆕 | 조건 추가 (기존 자유 작성 → 제약) |
| 리뷰 | 리뷰 조회/삭제 | 그대로 유지 |

### 4-2. 강사 (Instructor) 기능 🆕

| 카테고리 | 기능 | 엔드포인트 |
|----------|------|-----------|
| 강의 관리 | 내 강의 목록 조회 | `GET /api/instructor/courses` |
| 강의 관리 | 강의 생성 (DRAFT 상태) | `POST /api/instructor/courses` |
| 강의 관리 | 강의 수정 (소유자만) | `PUT /api/instructor/courses/{id}` |
| 강의 관리 | 강의 삭제 (soft delete) | `DELETE /api/instructor/courses/{id}` |
| 강의 관리 | 강의 발행 / 보관 | `POST /api/instructor/courses/{id}/publish` · `/archive` |
| 커리큘럼 | 섹션 추가/수정/삭제/순서 변경 | `/api/instructor/courses/{courseId}/sections/...` |
| 커리큘럼 | 렉처 추가/수정/삭제/순서 변경 | `/api/instructor/sections/{sectionId}/lectures/...` |
| 운영 | 내 강의 수강생 목록 | `GET /api/instructor/courses/{id}/students` |
| 운영 | 내 강의 리뷰 조회 | `GET /api/instructor/courses/{id}/reviews` |
| 운영 | 강사 대시보드 (요약 통계) | `GET /api/instructor/dashboard` |
| 프로필 | 강사 프로필 수정 | `PUT /api/instructor/profile` |
| 매출 🆕 | 내 강의 매출/환불 집계 | `GET /api/instructor/dashboard` (revenue 필드) |
| 운영 🆕 | 강의 취소 처리 (일괄 환불 트리거) | `POST /api/instructor/courses/{id}/cancel` |

---

## 5. 비기능 요구사항

| 카테고리 | 요구사항 |
|----------|---------|
| **인증** | JWT (P1 그대로) — Access Token(HS256) + Refresh Token(DB 저장, rotation) |
| **인가** | `@PreAuthorize("hasRole('INSTRUCTOR')")` + 서비스 레이어 소유자 검증. IDOR 반드시 방어 |
| **성능** | 강의 목록 API 200ms 이하, 대시보드 API 500ms 이하 (P1 기준 유지) |
| **보안** | OWASP A01/A02/A03/A07 핵심 점검. 비밀번호 BCrypt(기존 유지), JWT secret env 주입(기존 유지) |
| **가용성** | 단일 인스턴스 전제. `GET /api/health` 헬스 엔드포인트 유지 |
| **국제화** | 한국어 UI/에러 메시지 (P1 규격 유지) |
| **호환성** | P1 클라이언트가 갱신 전에도 동작 가능하도록 기존 엔드포인트는 **응답 포맷 하위호환** 유지 (필드 추가만) |

---

## 6. P1 → P2 Before/After

| 항목 | P1 (현재) | P2 (목표) |
|------|----------|----------|
| 가입 | `username`, `password` 2필드 | `role` 선택 추가 (`STUDENT` default, `INSTRUCTOR` 선택 가능) |
| 사용자 엔티티 | `User(id, username, password)` | `User` + `role` enum + 강사는 `InstructorProfile` 연결 |
| 강의 생성자 추적 | `courses.instructor_name` 문자열 컬럼 (자유 기입) | `courses.instructor_id` FK (`users.id`) + 하위호환용 `instructor_name` 유지 후 deprecate |
| 강의 가격 | 없음 (전부 무료 가정) | `courses.price` (BIGINT, 원 단위, ≥ 0) 추가. 0 = 무료 |
| 강의 CRUD 권한 | 인증만 되면 누구나 POST/PUT/DELETE 가능 (SecurityConfig상) | `INSTRUCTOR` + **소유자**만 수정/삭제 |
| 강의 발행 상태 | 없음 (만들자마자 노출) | `publish_status` enum (`DRAFT`/`PUBLISHED`/`ARCHIVED`). 학생 목록엔 PUBLISHED만 |
| 수강 진입 | `POST /api/enrollments` (무료) | **결제 플로우 경유** (장바구니 → 주문 → 모의 결제 → 자동 enrollment 생성). 무료 강의도 동일 플로우로 0원 결제 |
| 거래 기록 | 없음 | `orders`, `order_items`, `payments`, `refunds` 테이블에 이벤트 축적 |
| 리뷰 작성 조건 | 로그인만 되면 누구나 | **해당 강의 진도율 80% 이상** 학생만 |
| 학습 UX | 렉처 클릭 → 처음부터 재생 | **이어듣기** (재생 위치 DB 저장), **북마크** (즐겨찾기 + 메모) |
| 프론트 라우트 | `/login /signup /courses /courses/:id /my/courses /courses/:id/learn/:enrollmentId` | + `/instructor/*`, `/instructors/:id`, **`/cart` `/orders` `/orders/:id`** |
| 라우트 가드 | `ProtectedRoute` (로그인 여부만) | + `RoleGuard` (role 확인) |
| JWT 클레임 | `sub = userId` | `sub = userId`, `role = STUDENT|INSTRUCTOR` (Spring Security Authorities로 매핑) |
| `UserPrincipal.getAuthorities()` | `List.of()` (빈 리스트) | `List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))` |

---

## 7. 범위 밖 (P3 이후)

다음은 **P2에서 다루지 않는다.** 인터페이스만 막히지 않도록 둔다.

- **실제 PG 연동** (토스페이먼츠, KG이니시스 등) — P2는 **모의 결제**. SDK·콜백·webhook 없음
- 쿠폰 / 할인 코드 / 장바구니 프로모션
- 수익 정산 / 강사 송금 / 세금 계산서
- 구독(이메일/디스코드) / webhook
- 스케줄러, 배치 작업 (정기 결제, 만료 알림)
- Sentry / OpenTelemetry 모니터링
- 배포 자동화(CI/CD)
- 성능 최적화(인덱스 튜닝, 캐시, 코드 스플리팅)
- 동영상 스트리밍 인프라 (HLS, CDN) — URL만 저장, 재생 위치만 기록
- 동영상 검색·자막·자동화

PDF 수업 계획의 P3 범위.

---

## 8. 산출물 문서 목록

본 P2 설계 문서 세트 (이 문서 포함 총 9개 + CLAUDE.md):

| # | 문서 | 내용 |
|---|------|------|
| 01 | project-overview.md | 본 문서 |
| 02 | data-model.md | ERD, 테이블 변경, 마이그레이션 전략 |
| 03 | api-design.md | Public / Student / Instructor API 전체 목록 |
| 04 | architecture.md | 레이어, Security 필터 체인, 디렉토리 구조 |
| 05 | sample-data.md | seed SQL (강사·강의·학생 샘플) |
| 06 | implementation-checklist.md | 주차별 Phase + DoD |
| 07 | instructor-api-spec.md | 강사 API 심화 스펙 (JSON/검증/IDOR 패턴) |
| 08 | student-feature-spec.md | 학생 측 변경점 상세 |
| 09 | security-design.md | STRIDE / OWASP / 권한 매트릭스 |
| — | `../CLAUDE.md` | Claude Code 작업 컨텍스트 |
