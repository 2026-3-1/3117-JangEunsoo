# 06. 구현 체크리스트 — DevLearn P2

P2 구현을 8개 Phase로 쪼갠 작업 체크리스트. 각 Phase는 **DoD(Definition of Done)** 를 명확히 가지며, 이전 Phase의 완료가 다음의 전제이다. PDF 수업계획 기준 5주차(설계) → 9주차(통합)에 맞춰 배분했다.

---

## 0. 전제와 원칙

- **코드 위치**: `d:/devlearn/P2/backend`, `d:/devlearn/P2/frontend` (P1 복사본)
- **DB**: `devlearn_p2` 스키마 신규 생성. P1 DB와 격리
- **하위호환**: 기존 P1 엔드포인트 경로·응답은 유지, 필드만 추가
- **TDD 우선**: 보안·권한 검증은 반드시 테스트부터

---

## 1. 주차별 배분 (PDF 일정 매핑)

| 주차 | 기간 (예시) | 집중 Phase |
|-----|-----------|----------|
| 5주차 | 설계·환경 | Phase 1, 2 시작 |
| 6주차 | | Phase 2, 3 |
| 7주차 | | Phase 3, 4 |
| 8주차 | P1/P2 통합 | Phase 5, 6 |
| 9주차 | 마감 | Phase 7, 8 |

---

## 2. Phase 1 — DB 마이그레이션 & User.role 도입

**목표**: 스키마를 P2 수준으로 끌어올리고, 모든 기존 사용자에게 기본 role을 부여한다.

### 체크리스트

- [ ] `devlearn_p2` MySQL 스키마 생성
- [ ] P2 backend `application.properties`에서 datasource URL을 `devlearn_p2`로 변경
- [ ] 애플리케이션 기동 → JPA `ddl-auto=update`가 기본 테이블 생성 확인
- [ ] `User` 엔티티에 `Role` enum 필드 추가 (default STUDENT)
  - [ ] `Role.java` (STUDENT | INSTRUCTOR) 작성
  - [ ] `User`에 `@Enumerated(EnumType.STRING)` 컬럼
- [ ] 기존 users 데이터 backfill: `UPDATE users SET role='STUDENT' WHERE role IS NULL;`
- [ ] `InstructorProfile` 엔티티 + 테이블 생성 (02-data-model §4 참조)
- [ ] `Course`에 `instructor_id`, `publish_status`, `published_at` 추가
  - [ ] `PublishStatus` enum 작성
  - [ ] 기존 courses를 `PUBLISHED` + 기본 instructor (seed의 instructor1=id 1)로 backfill
  - [ ] `instructor_id` NOT NULL 제약 추가
- [ ] 인덱스 생성: `idx_courses_instructor`, `idx_courses_publish`
- [ ] 05-sample-data.md의 seed SQL 적용
- [ ] 수동 확인: SELECT로 role 분포·course.publish_status 분포 점검

### DoD

- `users` 테이블의 모든 row가 role ∈ {STUDENT, INSTRUCTOR}
- `courses.instructor_id`가 NULL인 row 0건
- seed 적용 후 `instructor1` 계정으로 로그인했을 때 JWT 정상 발급

---

## 3. Phase 2 — Spring Security 역할 기반 인가

**목표**: JWT에 role을 실은 뒤, URL·메서드 레벨 권한 게이트를 심는다.

### 체크리스트

- [ ] `UserPrincipal.getAuthorities()` 수정: `ROLE_` + role 반환
  ```java
  return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
  ```
- [ ] `TokenProvider`에 role 클레임 추가 (선택이지만 권장)
- [ ] `JwtAuthFilter`가 토큰에서 role을 읽어 `UserPrincipal`에 주입
- [ ] `SecurityConfig` URL 규칙 재정비:
  - [ ] `GET /api/courses/**`, `/api/categories/**`, `/api/instructors/{id}` permitAll
  - [ ] `POST/PUT/DELETE /api/courses/**` 제거 (P2는 instructor 경로로 이동)
  - [ ] `/api/instructor/**` .hasRole("INSTRUCTOR")
- [ ] `@EnableMethodSecurity` 확인 (이미 P1에서 활성화됨)
- [ ] 모든 강사 전용 컨트롤러에 `@PreAuthorize("hasRole('INSTRUCTOR')")` 클래스 레벨 적용
- [ ] `OwnershipValidator` 유틸 작성 (07-instructor-api-spec §5 참조)
- [ ] 권한 우회 단위 테스트 3종 이상
  - [ ] 비로그인 강사 리소스 접근 → 401
  - [ ] STUDENT가 강사 리소스 접근 → 403
  - [ ] INSTRUCTOR가 남의 강의 수정 → 404

### DoD

- 권한 우회 테스트가 3종 이상 통과
- `/api/courses/**` POST/PUT/DELETE 라우트가 존재하지 않음 (instructor 경로로 완전 이동)

---

## 4. Phase 3 — 강사 강의 CRUD API

**목표**: 강사가 강의·섹션·렉처를 만들고 발행하는 백엔드 API 완성.

### 체크리스트

- [ ] `InstructorCourseController` + Service
  - [ ] `POST /api/instructor/courses` (DRAFT로 생성)
  - [ ] `GET /api/instructor/courses` (본인 강의 목록, status 필터)
  - [ ] `GET /api/instructor/courses/{id}` (본인 강의 상세)
  - [ ] `PUT /api/instructor/courses/{id}` (기본 정보 수정)
  - [ ] `DELETE /api/instructor/courses/{id}` (ARCHIVED로 전환, 물리 삭제 아님)
- [ ] `InstructorSectionController` + Service
  - [ ] 섹션 CRUD + sort_order 재정렬
- [ ] `InstructorLectureController` + Service
  - [ ] 렉처 CRUD + sort_order 재정렬
- [ ] `POST /api/instructor/courses/{id}/publish`
  - [ ] DRAFT → PUBLISHED 전환
  - [ ] 섹션 ≥ 1 && 렉처 ≥ 1 검증 (없으면 422)
  - [ ] `published_at` = now()
- [ ] `POST /api/instructor/courses/{id}/archive`
  - [ ] PUBLISHED → ARCHIVED 전환
- [ ] `InstructorErrorCode` enum 정의
- [ ] 모든 쓰기 API에 `OwnershipValidator.requireOwnedCourse()` 적용
- [ ] 공개 강의 조회(`GET /api/courses/**`)가 PUBLISHED만 반환하도록 `CourseRepository` 수정

### DoD

- Postman/curl로 end-to-end 발행 플로우 완주 (create → section → lecture → publish)
- 다른 강사의 강의를 수정 시도하면 404 반환

---

## 5. Phase 4 — 강사 대시보드 & 프로필 API

**목표**: 강사 콘솔 홈에서 보여줄 집계와 프로필 편집 API.

### 체크리스트

- [ ] `GET /api/instructor/dashboard`
  - [ ] 본인 강의 수 (상태별)
  - [ ] 누적 수강생 수
  - [ ] 평균 평점, 리뷰 수
  - [ ] 최근 수강생 5명
- [ ] `GET /api/instructor/courses/{id}/students`
  - [ ] 해당 강의의 수강생 목록 + 각자 진도율
- [ ] `GET /api/instructor/profile` (본인 프로필 조회)
- [ ] `PUT /api/instructor/profile` (bio, career_years, profile_image_url 수정)
  - [ ] display_name은 수정 가능하되 다른 필드와 validation
- [ ] `GET /api/instructors/{userId}` (public)
  - [ ] 존재하지만 role=STUDENT 면 404
  - [ ] PUBLISHED 강의만 함께 반환
  - [ ] 공개 통계 (수강생 수, 평균 평점) 포함
- [ ] `GET /api/auth/me`
  - [ ] userId, username, role 반환

### DoD

- 강사 로그인 → `/dashboard`에서 숫자 표시 정상
- `/api/instructors/5` (student1) 조회 시 404
- `/api/instructors/1` (instructor1) 조회 시 프로필 + 2개 PUBLISHED 강의

---

## 6. Phase 5 — 프론트 공통 기반 (AuthContext · RoleGuard · 라우팅)

**목표**: 프론트의 인증·인가 기반을 P2 수준으로 업그레이드.

### 체크리스트

- [ ] `AuthContext` 확장
  - [ ] `userId`, `role` 필드 추가
  - [ ] `login()` 성공 후 `GET /api/auth/me` 호출하여 role 세팅
  - [ ] 새로고침 시 저장된 토큰으로 `/auth/me` 재호출
- [ ] `RoleGuard` 컴포넌트 작성 (08-student-feature-spec §4-3 참조)
- [ ] `App.tsx` 라우팅 확장 (08 §4-2)
- [ ] `SignupPage`에 role 선택 UI (학생 기본, 강사 선택 시 displayName/bio/career 필드 확장)
- [ ] `NavBar` 조건부 렌더링 (08 §6)
  - [ ] role === 'INSTRUCTOR' 일 때 "강사 콘솔" 메뉴
- [ ] `api/instructor.ts` Axios 클라이언트 신설
- [ ] `api/auth.ts`에 `fetchMe()` 추가

### DoD

- 학생으로 로그인 후 수동으로 `/instructor/dashboard` 입력 → `/courses`로 리다이렉트
- 강사로 로그인 후 네비게이션 바에 "강사 콘솔" 노출 확인

---

## 7. Phase 6 — 강사 콘솔 UI (강의 생성·편집·발행)

**목표**: 강사가 GUI로 강의를 관리할 수 있게 한다.

### 체크리스트

- [ ] `InstructorDashboardPage` — 대시보드 카드 4개 + 최근 수강생
- [ ] `InstructorCourseListPage` — 본인 강의 테이블, status 필터, "새 강의" 버튼
- [ ] `InstructorCourseEditorPage` (신규 `/instructor/courses/new` + 편집 `/instructor/courses/:id/edit`)
  - [ ] 기본 정보 폼 (제목, 설명, 난이도, 카테고리)
  - [ ] 섹션 리스트 + 추가/수정/삭제
  - [ ] 렉처 리스트 + 추가/수정/삭제
  - [ ] 섹션/렉처 드래그 정렬 (최소: 위/아래 버튼)
  - [ ] 발행 버튼 (status === DRAFT 일 때)
  - [ ] 보관 버튼 (status === PUBLISHED 일 때)
  - [ ] 발행 전 validation 에러 (422) 친절히 표시
- [ ] `InstructorCourseStudentsPage` — 수강생 테이블 + 진도율 막대

### DoD

- 강사가 "새 강의" → 정보 입력 → 섹션 2개·렉처 3개 추가 → 발행까지 브라우저에서 완주 가능
- 발행 후 학생 계정으로 `/courses` 열면 해당 강의 노출

---

## 8. Phase 7 — 강사 프로필 공개 페이지 & 학생 UI 변경

**목표**: 학생 측 UX 변경 반영 + 강사 공개 프로필 페이지.

### 체크리스트

- [ ] `CoursesPage` 카드에 강사 이름 링크 (`/instructors/:id`)
- [ ] `CourseDetailPage` 에 `InstructorCard` 컴포넌트 삽입
- [ ] `InstructorPublicProfilePage` (`/instructors/:userId`) 구현
  - [ ] 프로필 헤더 (display_name, career_years, profile_image, bio)
  - [ ] 개설 강의 그리드 (PUBLISHED만)
  - [ ] 공개 통계 (수강생 수, 평균 평점, 리뷰 수)
  - [ ] 404 케이스 핸들링 (role=STUDENT)
- [ ] `MyCoursesPage` 카드에도 강사 링크
- [ ] `InstructorProfileEditPage` (`/instructor/profile`) — 본인 프로필 편집

### DoD

- 학생이 강의 카드 → 강사 이름 → 강사 프로필 → 다른 강의 카드 → 강의 상세, 네비게이션 완주
- 강사가 본인 프로필 bio를 수정하면 공개 페이지에 즉시 반영

---

## 9. Phase 8 — E2E 테스트 & 통합

**목표**: 권한 우회 회귀 테스트, P1/P2 통합 점검, 데모 준비.

### 체크리스트

- [ ] 백엔드 통합 테스트
  - [ ] 비인증으로 `/api/instructor/**` 호출 → 401
  - [ ] STUDENT 토큰으로 `/api/instructor/**` 호출 → 403
  - [ ] INSTRUCTOR A의 토큰으로 INSTRUCTOR B 강의 PUT/DELETE → 404
  - [ ] DRAFT 강의가 `/api/courses` 목록에 포함되지 않음
  - [ ] DRAFT 강의 상세를 소유자 아닌 자가 조회 → 404
  - [ ] 발행 전 섹션/렉처가 없는 강의를 publish 시도 → 422
- [ ] 프론트 라우팅 회귀
  - [ ] 비로그인 `/instructor/*` → `/login`
  - [ ] STUDENT `/instructor/*` → `/courses`
- [ ] 데모 시나리오 3종(05-sample-data §9) 수동 완주
- [ ] README / CLAUDE.md 최신화
- [ ] (선택) 기본 성능 점검: 카드 10개 목록 SQL N+1 여부 점검

### DoD

- 권한 우회 테스트가 **전부 녹색**
- PDF 제출용 데모 영상 녹화 가능한 상태
- 문서(P2/docs/) 와 실제 코드의 엔드포인트 일치

---

## 10. 진행 현황 보드 (상태 업데이트용)

> 각 Phase 상태 컬럼을 PRD 리뷰 때마다 갱신한다.

| Phase | 이름 | 상태 | 메모 |
|-------|------|-----|-----|
| 1 | DB 마이그레이션 & User.role | TODO | |
| 2 | Spring Security 역할 인가 | TODO | |
| 3 | 강사 강의 CRUD API | TODO | |
| 4 | 강사 대시보드 & 프로필 API | TODO | |
| 5 | 프론트 공통 기반 | TODO | |
| 6 | 강사 콘솔 UI | TODO | |
| 7 | 공개 프로필 & 학생 UI | TODO | |
| 8 | E2E 테스트 & 통합 | TODO | |

---

## 11. 범위 밖 (P3 이월)

다음 항목은 본 체크리스트에서 **명시적으로 제외**한다:

- 결제 / 구독 / 쿠폰
- 강의 평점 기반 추천 (ML)
- 강의 비디오 실제 업로드·스트리밍 (URL만 저장)
- 알림/이메일
- Sentry·APM 연동
- CI/CD 파이프라인
- 성능 최적화 (캐싱, CDN)

---

## 12. 관련 문서

- 전체 개요: [01-project-overview.md](./01-project-overview.md)
- 데이터 모델: [02-data-model.md](./02-data-model.md)
- API 설계: [03-api-design.md](./03-api-design.md)
- 아키텍처: [04-architecture.md](./04-architecture.md)
- 보안 설계: [09-security-design.md](./09-security-design.md)
