# CLAUDE.md — DevLearn P2

Claude Code 작업 컨텍스트. P2는 **역할 기반 접근제어(INSTRUCTOR vs STUDENT)와 강사용 기능 확장**이 핵심. JWT 인증 자체는 P1에서 이미 구현됨.

## Project

**devlearn P2** — 2026년 3학년 1학기 프로젝트 실습 수업의 인강사이트. P1(JWT 인증·강의 목록·수강·진도·리뷰)을 기반으로 **강사 역할**을 도입하여 강사가 강의를 등록·편집·발행·관리하는 기능을 추가한다. 학생과 강사가 공존하는 커뮤니티형 플랫폼으로 확장.

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
│   ├── course/        (Course + PublishStatus, Section, Lecture)
│   ├── category/
│   ├── enrollment/
│   ├── progress/
│   └── review/
└── global/
    ├── security/      (SecurityConfig, UserPrincipal, OwnershipValidator, jwt/)
    ├── error/         (CustomException, ErrorCode, InstructorErrorCode)
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
│   └── instructor/                          🆕
│       ├── InstructorDashboardPage.tsx
│       ├── InstructorCourseListPage.tsx
│       ├── InstructorCourseEditorPage.tsx
│       ├── InstructorCourseStudentsPage.tsx
│       └── InstructorProfileEditPage.tsx
└── api/
    ├── auth.ts  (fetchMe 추가)
    └── instructor.ts  🆕
```

### 주요 엔티티 관계 (P2)

```
User ──1:1── InstructorProfile       (role=INSTRUCTOR 만)
  │
  │ 1:N (instructor_id)
  ▼
Course ─── PublishStatus (DRAFT | PUBLISHED | ARCHIVED)
  │
  ├── 1:N ── Section ── 1:N ── Lecture
  │
  ├── 1:N ── Enrollment ── 1:N ── LectureProgress
  │              │
  │              owner: User(STUDENT 또는 INSTRUCTOR 둘 다 수강 가능)
  │
  └── 1:N ── Review
```

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
| 8 | E2E 테스트 & 통합 | TODO |

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

주요 P2 에러 코드: `COURSE_NOT_FOUND`(404, 소유자 아닐 때도 사용), `ACCESS_DENIED`(403), `PUBLISH_VALIDATION_FAILED`(422), `ALREADY_PUBLISHED`(409).

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

---

## Key Relationships

- `User.role` (enum STUDENT | INSTRUCTOR) — default STUDENT
- `User` 1:1 `InstructorProfile` — role=INSTRUCTOR 인 유저만
- `Course.instructor_id` → `User.id` (FK)
- `Course.publish_status` (enum DRAFT | PUBLISHED | ARCHIVED)
- 학생/강사 모두 `Enrollment`·`Review` 작성 가능 (역할 무관)
- **소유자 검증**: `course.instructor_id == currentUserId` 를 Service 계층에서 반드시 확인

---

## 코드 작업 시 주의

1. **새 컨트롤러 만들 때**: 강사 전용이면 **반드시** 클래스 레벨 `@PreAuthorize("hasRole('INSTRUCTOR')")` + Service에서 `OwnershipValidator` 호출
2. **DRAFT 은폐**: `CourseRepository`에서 학생 공개 조회는 `publish_status = 'PUBLISHED'` 필터 필수
3. **하위호환**: 기존 `/api/courses`, `/api/enrollments` 등 P1 경로·응답 변경 금지. 필드 **추가만**
4. **에러 코드**: 다른 강사의 리소스 접근 시 **403이 아닌 404** (존재 자체를 노출하지 않음)
5. **P1 건드리지 말 것**: `../P1/` 아래 코드는 읽기만
6. **한국어**: 커밋 메시지·문서·에러 메시지는 한국어 (P1 컨벤션 유지)

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
