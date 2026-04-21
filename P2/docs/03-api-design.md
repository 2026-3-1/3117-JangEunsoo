# 03. API 설계 — DevLearn P2

## 1. 공통 규격

### 1-1. Base URL
- 개발: `http://localhost:8080`
- 프론트 Vite dev 서버(5173)에서 `/api/**`는 백엔드로 프록시

### 1-2. 표준 응답 포맷 (`GlobalApiResponse<T>`, P1 그대로)

**성공**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": { /* T */ }
}
```

**실패**
```json
{
  "success": false,
  "status": 404,
  "message": "강의를 찾을 수 없습니다."
}
```

### 1-3. 인증 헤더

```
Authorization: Bearer <accessToken>
```

- `accessToken`: `POST /api/auth/login` 또는 `/refresh`의 응답
- Token payload: `sub = userId` (P1 그대로). **role은 JWT에 넣지 않고 매 요청마다 DB에서 조회** (security-design §4-2 참조)

### 1-4. 에러 코드 (공통)

| code | HTTP | 설명 |
|------|------|------|
| `VALIDATION_ERROR` | 400 | 요청 본문 유효성 실패 |
| `UNAUTHORIZED` | 401 | 토큰 없음/무효/만료 |
| `FORBIDDEN` | 403 | role 부족 |
| `NOT_FOUND` | 404 | 리소스 없음 (IDOR 은폐 포함) |
| `DUPLICATE` | 409 | 중복 |
| `SERVER_ERROR` | 500 | 내부 오류 |

도메인별 코드는 기존 `CourseErrorCode`, `AuthErrorCode`, `UserErrorCode`, `EnrollmentErrorCode`, `ProgressErrorCode`, `ReviewErrorCode` 활용 + P2에서 `InstructorErrorCode` 신설.

---

## 2. 엔드포인트 전체 목록

### 2-0. Legend

| 기호 | 의미 |
|------|------|
| 🆕 | P2 신규 |
| ♻ | P2에서 동작·응답 변경 |
| ⚠ | P2에서 제거/이동 |
| 🟢 | 변경 없음 (P1 그대로) |

### 2-1. Public (인증 불필요)

| 메서드 | 경로 | 설명 | 변경 |
|--------|------|------|------|
| GET | `/api/health` | 헬스체크 | 🟢 |
| POST | `/api/auth/signup` | 회원가입 | ♻ role 필드 추가 |
| POST | `/api/auth/login` | 로그인 | 🟢 |
| POST | `/api/auth/refresh` | 토큰 갱신 | 🟢 |
| GET | `/api/categories` | 카테고리 목록 | 🟢 |
| GET | `/api/courses` | 강의 목록 (검색/필터/페이지) | ♻ PUBLISHED만 반환 |
| GET | `/api/courses/{id}` | 강의 상세 | ♻ DRAFT/ARCHIVED는 소유자 아니면 404 |
| GET | `/api/reviews/courses/{courseId}` | 특정 강의의 리뷰 | 🟢 |
| GET | `/api/instructors/{userId}` | **강사 공개 프로필** | 🆕 |

### 2-2. Student / 공통 인증 (STUDENT + INSTRUCTOR 모두 가능)

| 메서드 | 경로 | 설명 | 변경 |
|--------|------|------|------|
| POST | `/api/auth/logout` | 로그아웃 | 🟢 |
| POST | `/api/enrollments` | 수강 신청 | 🟢 |
| GET | `/api/enrollments/me` | 내 수강 목록 | 🟢 |
| GET | `/api/enrollments/{id}/progress-rate` | 진도율 | 🟢 |
| DELETE | `/api/enrollments/{id}` | 수강 취소 | 🟢 |
| POST | `/api/progress/complete` | 렉처 완료 처리 | 🟢 |
| POST | `/api/reviews` | 리뷰 작성 | 🟢 |
| DELETE | `/api/reviews/{id}` | 리뷰 삭제 (작성자) | 🟢 |

### 2-3. Instructor 전용 (role = INSTRUCTOR)

모두 🆕. 베이스 경로: `/api/instructor/**`

#### 강의 관리

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/instructor/courses` | 내가 만든 강의 목록 (DRAFT/PUBLISHED/ARCHIVED 전부) |
| POST | `/api/instructor/courses` | 강의 생성 (DRAFT) |
| GET | `/api/instructor/courses/{id}` | 내 강의 상세 (소유자 검증) |
| PUT | `/api/instructor/courses/{id}` | 강의 수정 |
| DELETE | `/api/instructor/courses/{id}` | 강의 소프트 삭제 |
| POST | `/api/instructor/courses/{id}/publish` | 발행 (DRAFT → PUBLISHED) |
| POST | `/api/instructor/courses/{id}/archive` | 보관 (PUBLISHED → ARCHIVED) |

#### 섹션 / 렉처

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/instructor/courses/{courseId}/sections` | 섹션 추가 |
| PUT | `/api/instructor/sections/{sectionId}` | 섹션 수정 (제목, 순서) |
| DELETE | `/api/instructor/sections/{sectionId}` | 섹션 삭제 |
| POST | `/api/instructor/sections/{sectionId}/lectures` | 렉처 추가 |
| PUT | `/api/instructor/lectures/{lectureId}` | 렉처 수정 |
| DELETE | `/api/instructor/lectures/{lectureId}` | 렉처 삭제 |

#### 운영 / 모니터링

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/instructor/courses/{id}/students` | 수강생 목록 |
| GET | `/api/instructor/courses/{id}/reviews` | 내 강의 리뷰 (페이지네이션) |
| GET | `/api/instructor/dashboard` | 대시보드 요약 (강의수, 누적 수강생, 평균 평점) |

#### 프로필

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/instructor/profile` | 내 프로필 조회 |
| PUT | `/api/instructor/profile` | 내 프로필 수정 (display_name / bio / career_years / image) |

### 2-4. P2에서 제거/이동되는 P1 엔드포인트 ⚠

| P1 엔드포인트 | P2 조치 |
|--------------|--------|
| `POST /api/courses` | **제거** → `POST /api/instructor/courses`로 이동 |
| `PUT /api/courses/{id}` | **제거** → `PUT /api/instructor/courses/{id}` |
| `DELETE /api/courses/{id}` | **제거** → `DELETE /api/instructor/courses/{id}` |

(카테고리 쓰기 엔드포인트는 P1에도 존재하나, P2에선 **일단 유지하되 운영용(internal) 마킹**. 공개 관리 UI는 P3에서 Admin 역할과 함께 설계)

---

## 3. 주요 엔드포인트 상세

### 3-1. POST `/api/auth/signup` (♻ 변경)

**요청**
```json
{
  "username": "alice",
  "password": "P@ssw0rd!",
  "role": "INSTRUCTOR",
  "displayName": "앨리스 강사",
  "bio": "10년차 백엔드 엔지니어"
}
```

**필드**
| 필드 | 타입 | 필수 | 비고 |
|------|------|------|------|
| `username` | string (3-15) | ✅ | P1 그대로, unique |
| `password` | string (8-64) | ✅ | P1 그대로, BCrypt 해싱 |
| `role` | `"STUDENT"` \| `"INSTRUCTOR"` | ❌ (default `"STUDENT"`) | 허용 외 값은 400 |
| `displayName` | string (1-50) | `role=INSTRUCTOR`일 때 ✅ | 학생이면 무시 |
| `bio` | string | ❌ | |

**응답 — 201 Created**
```json
{
  "success": true,
  "status": 201,
  "message": "Sign-up completed successfully.",
  "data": null
}
```

**에러**
- 409 `DUPLICATE_USERNAME` — username 중복
- 400 `VALIDATION_ERROR` — 형식 위반, role=INSTRUCTOR인데 displayName 없음

### 3-2. POST `/api/auth/login` (🟢 그대로)

**요청**
```json
{ "username": "alice", "password": "P@ssw0rd!" }
```

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

(role은 토큰이 아닌, 아래 프로필 API로 확인)

### 3-3. GET `/api/courses` (♻ 변경 — PUBLISHED만)

**쿼리 파라미터**
| 파라미터 | 타입 | 비고 |
|----------|------|------|
| `categoryId` | Long | optional |
| `difficulty` | `"BEGINNER"` \| `"INTERMEDIATE"` \| `"ADVANCED"` | optional |
| `keyword` | string | 제목 LIKE |
| `page` / `size` / `sort` | Pageable | default size=12 |

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "스프링부트 입문",
        "description": "...",
        "difficulty": "BEGINNER",
        "instructorId": 5,
        "instructorName": "앨리스 강사",
        "categoryId": 2,
        "publishedAt": "2026-04-01T12:00:00"
      }
    ],
    "totalElements": 38,
    "totalPages": 4,
    "number": 0,
    "size": 12
  }
}
```

**변경점**
- `publish_status = 'PUBLISHED'` 필터 필수
- 응답에 `instructorId`(FK), `publishedAt` 추가
- `instructorName`은 `InstructorProfile.display_name` 또는 기존 컬럼 fallback

### 3-4. GET `/api/courses/{id}` (♻ 변경)

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "id": 1,
    "title": "스프링부트 입문",
    "description": "...",
    "difficulty": "BEGINNER",
    "categoryId": 2,
    "categoryName": "백엔드",
    "instructor": {
      "userId": 5,
      "displayName": "앨리스 강사",
      "bio": "10년차 백엔드 엔지니어",
      "careerYears": 10,
      "profileImageUrl": null
    },
    "publishStatus": "PUBLISHED",
    "publishedAt": "2026-04-01T12:00:00",
    "sections": [
      {
        "id": 10,
        "title": "섹션 1",
        "orderNum": 1,
        "lectures": [
          { "id": 100, "title": "강의 1-1", "videoUrl": "...", "orderNum": 1 }
        ]
      }
    ]
  }
}
```

**에러**
- 404 `COURSE_NOT_FOUND` — 미존재 or 소유자 아닌데 DRAFT/ARCHIVED

### 3-5. GET `/api/instructors/{userId}` (🆕)

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "userId": 5,
    "username": "alice",
    "displayName": "앨리스 강사",
    "bio": "10년차 백엔드 엔지니어",
    "careerYears": 10,
    "profileImageUrl": null,
    "publishedCourseCount": 3,
    "totalStudentCount": 142,
    "averageRating": 4.6
  }
}
```

**에러**
- 404 — userId가 INSTRUCTOR 아니면 은폐

### 3-6. POST `/api/instructor/courses` (🆕)

**헤더**: `Authorization: Bearer ...` (role=INSTRUCTOR)

**요청**
```json
{
  "title": "스프링부트 입문",
  "description": "기초부터 차근차근",
  "difficulty": "BEGINNER",
  "categoryId": 2
}
```

**응답 — 201**
```json
{
  "success": true,
  "status": 201,
  "message": "요청 성공",
  "data": {
    "id": 1,
    "title": "스프링부트 입문",
    "instructorId": 5,
    "publishStatus": "DRAFT",
    "publishedAt": null
  }
}
```

### 3-7. PUT `/api/instructor/courses/{id}` (🆕)

요청은 3-6과 유사. **소유자 아니면 404.**

### 3-8. POST `/api/instructor/courses/{id}/publish` (🆕)

**요청 본문 없음**

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "강의가 발행되었습니다.",
  "data": {
    "id": 1,
    "publishStatus": "PUBLISHED",
    "publishedAt": "2026-04-20T10:00:00"
  }
}
```

**규칙**
- DRAFT → PUBLISHED 전환만 허용 (ARCHIVED에서 재발행은 별도 정책, 현재 불허)
- 발행 전 검증: 강의에 최소 1개 섹션 + 1개 렉처 존재해야 함. 아니면 400 `COURSE_NOT_READY`

### 3-9. POST `/api/instructor/courses/{courseId}/sections` (🆕)

**요청**
```json
{ "title": "섹션 1: 환경 세팅", "orderNum": 1 }
```

**응답 — 201**: 새 `SectionResponseDTO`

### 3-10. POST `/api/instructor/sections/{sectionId}/lectures` (🆕)

**요청**
```json
{
  "title": "강의 1-1: IntelliJ 설치",
  "videoUrl": "https://www.youtube.com/watch?v=xxx",
  "orderNum": 1
}
```

### 3-11. GET `/api/instructor/courses/{id}/students` (🆕)

**쿼리**: `page`, `size` (default 20)

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "content": [
      {
        "userId": 8,
        "username": "student01",
        "enrolledAt": "2026-04-10T09:00:00",
        "progressRate": 35.7,
        "completedLectureCount": 5,
        "totalLectureCount": 14
      }
    ],
    "totalElements": 142,
    "totalPages": 8
  }
}
```

### 3-12. GET `/api/instructor/dashboard` (🆕)

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "요청 성공",
  "data": {
    "courseCount": {
      "draft": 1,
      "published": 3,
      "archived": 0
    },
    "totalStudents": 142,
    "totalReviews": 37,
    "averageRating": 4.6,
    "recentEnrollments": [
      {
        "userId": 8,
        "username": "student01",
        "courseId": 1,
        "courseTitle": "스프링부트 입문",
        "enrolledAt": "2026-04-19T23:59:00"
      }
    ]
  }
}
```

### 3-13. PUT `/api/instructor/profile` (🆕)

**요청**
```json
{
  "displayName": "앨리스 강사",
  "bio": "10년차 백엔드 엔지니어",
  "careerYears": 10,
  "profileImageUrl": "https://.../alice.png"
}
```

---

## 4. 요청 파이프라인

```
Client (React, axios)
       │   Authorization: Bearer <access>
       ▼
┌─────────────────────────────────────────┐
│ CORS Filter                             │  (SecurityConfig.corsConfigurationSource)
└─────────────┬───────────────────────────┘
              ▼
┌─────────────────────────────────────────┐
│ JwtAuthenticationFilter                 │
│  - Authorization 헤더 파싱              │
│  - tokenProvider.validateToken()        │
│  - CustomUserDetailsService.loadById()  │  ← DB에서 role 재조회
│  - SecurityContext에 UserPrincipal 세팅 │     (신뢰 원천은 DB)
└─────────────┬───────────────────────────┘
              ▼
┌─────────────────────────────────────────┐
│ SecurityFilterChain authorization       │
│  - permitAll / authenticated 체크       │
└─────────────┬───────────────────────────┘
              ▼
┌─────────────────────────────────────────┐
│ @PreAuthorize("hasRole('INSTRUCTOR')")  │  ← 컨트롤러 메서드 직전
└─────────────┬───────────────────────────┘
              ▼
┌─────────────────────────────────────────┐
│ Controller → Service                    │
│  - Bean Validation (@Valid)             │
│  - OwnershipValidator.requireOwnedXxx() │  ← IDOR 방어 (쓰기 경로)
│  - 비즈니스 로직                         │
└─────────────┬───────────────────────────┘
              ▼
┌─────────────────────────────────────────┐
│ GlobalExceptionHandler (CustomException)│
│  - ErrorCode → GlobalApiResponse.fail() │
└─────────────────────────────────────────┘
```

---

## 5. Bean Validation 요약

| DTO | 필드 | 제약 |
|-----|------|------|
| `SignupRequestDTO` | `username` | `@NotBlank @Size(min=3, max=15)` |
| `SignupRequestDTO` | `password` | `@NotBlank @Size(min=8, max=64)` |
| `SignupRequestDTO` | `role` | `@Pattern(regexp = "STUDENT\|INSTRUCTOR")` (null 허용) |
| `SignupRequestDTO` | `displayName` | role=INSTRUCTOR일 때 `@NotBlank @Size(max=50)` — cross-field `@AssertTrue` |
| `CourseCreateRequestDTO` | `title` | `@NotBlank @Size(max=200)` |
| `CourseCreateRequestDTO` | `difficulty` | `@Pattern(regexp = "BEGINNER\|INTERMEDIATE\|ADVANCED")` |
| `SectionCreateRequestDTO` | `title` | `@NotBlank @Size(max=100)` |
| `SectionCreateRequestDTO` | `orderNum` | `@Min(1)` |
| `LectureCreateRequestDTO` | `videoUrl` | `@NotBlank @Pattern` (http/https) |
| `InstructorProfileUpdateRequestDTO` | `displayName` | `@NotBlank @Size(max=50)` |

---

## 6. OpenAPI 통합

- P1과 같이 `springdoc-openapi-starter-webmvc-ui` 사용
- 개발 환경에서 `SWAGGER_ENABLED=true`로 토글
- 강사 전용 엔드포인트는 `@Tag(name = "Instructor")`로 그룹
- 각 컨트롤러 메서드에 `@Operation(summary, description)` + `@ApiResponse` 표기
- JWT 인증: `@SecurityRequirement(name = "bearerAuth")`

---

## 7. 엔드포인트 × 역할 요약표

(09-security-design §3 권한 매트릭스와 동일. 여기선 API 관점에서 간추림)

| 카테고리 | 엔드포인트 수 | 인증 | Role 제약 | 추가 체크 |
|----------|--------------|------|----------|----------|
| Auth | 4 | 부분 | — | — |
| Public (조회) | 5 | ❌ | — | `publish_status=PUBLISHED` 필터 |
| Student/공통 | 8 | ✅ | — | 작성자/수강자 본인 |
| Instructor | 17 | ✅ | `INSTRUCTOR` | **소유자 검증** |

총 **34개** 엔드포인트 (P1의 16개 + P2 신규/변경).
