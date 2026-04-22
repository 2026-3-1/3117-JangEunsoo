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
| GET | `/api/auth/me` | 내 식별정보 (userId, username, role) | 🆕 |
| POST | `/api/enrollments` | 수강 신청 (P1 호환용, 무료 강의만) | ♻ 가격>0이면 400 (주문 경유 요구) |
| GET | `/api/enrollments/me` | 내 수강 목록 | 🟢 |
| GET | `/api/enrollments/{id}/progress-rate` | 진도율 | 🟢 |
| DELETE | `/api/enrollments/{id}` | 수강 취소 | 🟢 |
| POST | `/api/progress/complete` | 렉처 완료 처리 | 🟢 |
| POST | `/api/reviews` | 리뷰 작성 | ♻ 진도 80% 이상만 허용 |
| DELETE | `/api/reviews/{id}` | 리뷰 삭제 (작성자) | 🟢 |
| **장바구니** | | | 🆕 |
| GET | `/api/cart` | 내 장바구니 조회 | 🆕 |
| POST | `/api/cart/items` | 강의 담기 | 🆕 |
| DELETE | `/api/cart/items/{courseId}` | 개별 삭제 | 🆕 |
| DELETE | `/api/cart` | 비우기 | 🆕 |
| **주문/결제** | | | 🆕 |
| POST | `/api/orders` | 장바구니 스냅샷으로 주문 생성 (`PENDING`) | 🆕 |
| POST | `/api/payments/checkout` | 모의 결제 실행 → 주문 `PAID` + enrollment 자동 생성 | 🆕 |
| GET | `/api/orders` | 내 주문 내역 | 🆕 |
| GET | `/api/orders/{id}` | 주문 상세 | 🆕 |
| POST | `/api/orders/{id}/refund` | 주문 환불 요청 (전체/부분) | 🆕 |
| **이어듣기** | | | 🆕 |
| PUT | `/api/playback` | 재생 위치 upsert | 🆕 |
| GET | `/api/playback/lectures/{lectureId}` | 특정 렉처의 내 재생 위치 | 🆕 |
| GET | `/api/playback/enrollments/{id}/resume` | 해당 수강 건의 "이어볼 렉처+위치" | 🆕 |
| **북마크** | | | 🆕 |
| POST | `/api/bookmarks` | 북마크 추가 | 🆕 |
| GET | `/api/bookmarks/lectures/{lectureId}` | 특정 렉처의 내 북마크 목록 | 🆕 |
| GET | `/api/bookmarks/me` | 내 전체 북마크 | 🆕 |
| PUT | `/api/bookmarks/{id}` | 메모/위치 수정 | 🆕 |
| DELETE | `/api/bookmarks/{id}` | 북마크 삭제 | 🆕 |

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
| GET | `/api/instructor/dashboard` | 대시보드 요약 (강의수, 수강생, 평점, **매출/환불**) |
| POST | `/api/instructor/courses/{id}/cancel` | 강의 취소 → 해당 강의의 모든 PAID 주문 환불 트리거 🆕 |

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

### 3-14. GET `/api/auth/me` (🆕)

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": { "userId": 5, "username": "alice", "role": "INSTRUCTOR" }
}
```

---

## 3-A. 장바구니 (🆕)

### 3-A-1. GET `/api/cart`

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "courseId": 1,
        "title": "스프링부트 입문",
        "instructorName": "김백엔드",
        "price": 49000,
        "thumbnailUrl": null,
        "addedAt": "2026-04-20T09:00:00"
      },
      {
        "courseId": 4,
        "title": "React 18 완전 정복",
        "instructorName": "이프론트",
        "price": 59000,
        "thumbnailUrl": null,
        "addedAt": "2026-04-20T09:10:00"
      }
    ],
    "totalAmount": 108000,
    "itemCount": 2
  }
}
```

### 3-A-2. POST `/api/cart/items`

**요청**
```json
{ "courseId": 1 }
```

**에러**
- 404 `COURSE_NOT_FOUND` — 강의 없음 또는 PUBLISHED 아님
- 409 `ALREADY_IN_CART` — 이미 담음
- 409 `ALREADY_ENROLLED` — 이미 수강 중

**응답 — 201**: 장바구니 전체 (3-A-1과 동일)

### 3-A-3. DELETE `/api/cart/items/{courseId}` · DELETE `/api/cart`

개별 삭제 / 비우기. 응답 `204 No Content`.

---

## 3-B. 주문 & 모의 결제 (🆕)

### 3-B-1. POST `/api/orders`

**동작**: 현재 장바구니 내용을 **스냅샷**으로 `orders` + `order_items` 생성. 상태 `PENDING`.

**요청 본문 없음.** (서버가 current user의 cart를 조회)

**응답 — 201**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "orderId": 77,
    "orderNo": "ORD-20260420-0077",
    "status": "PENDING",
    "totalAmount": 108000,
    "items": [
      { "courseId": 1, "title": "스프링부트 입문", "price": 49000 },
      { "courseId": 4, "title": "React 18 완전 정복", "price": 59000 }
    ],
    "createdAt": "2026-04-20T09:20:00"
  }
}
```

**에러**
- 400 `EMPTY_CART` — 장바구니 비었음
- 409 `DUPLICATE_ENROLLMENT_IN_CART` — 이미 수강 중인 강의가 담겨 있음
- 409 `COURSE_NOT_PURCHASABLE` — DRAFT/ARCHIVED가 담겨 있음 (정합성 보호)

### 3-B-2. POST `/api/payments/checkout`

**동작** (모의):
1. `orderId` 받아 주문 조회 (소유자 검증)
2. `PENDING` 상태여야 함
3. `payments` 레코드 insert (`method=MOCK_CARD`, `status=SUCCESS`, `mock_transaction_id=UUID`)
4. `orders.status=PAID`, `paid_at=now()`
5. 각 `order_items`에 대해 **`enrollments` 자동 생성**
6. 장바구니 비우기

**요청**
```json
{ "orderId": 77 }
```

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "결제가 완료되었습니다.",
  "data": {
    "orderId": 77,
    "orderNo": "ORD-20260420-0077",
    "status": "PAID",
    "paidAt": "2026-04-20T09:21:00",
    "paymentId": 201,
    "mockTransactionId": "mock-4b2c-...",
    "enrollmentIds": [ 301, 302 ]
  }
}
```

**에러**
- 404 `ORDER_NOT_FOUND` — 남의 주문 은폐 포함
- 409 `ORDER_NOT_PAYABLE` — 이미 결제/취소/환불 상태

> 실패 시뮬레이션은 기본적으로 하지 않는다. 필요 시 `?simulateFailure=true` 쿼리로만 켤 수 있는 hook을 두어 `payments.status=FAILED` 기록 (과제용).

### 3-B-3. GET `/api/orders`

**쿼리**: `status`, `page`, `size`

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "orderId": 77,
        "orderNo": "ORD-20260420-0077",
        "status": "PAID",
        "totalAmount": 108000,
        "refundedAmount": 0,
        "itemCount": 2,
        "createdAt": "2026-04-20T09:20:00",
        "paidAt": "2026-04-20T09:21:00"
      }
    ],
    "totalElements": 1, "totalPages": 1, "number": 0, "size": 10
  }
}
```

### 3-B-4. GET `/api/orders/{id}`

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "orderId": 77,
    "orderNo": "ORD-20260420-0077",
    "status": "PAID",
    "totalAmount": 108000,
    "refundedAmount": 0,
    "items": [
      {
        "orderItemId": 501,
        "courseId": 1,
        "courseTitleSnapshot": "스프링부트 입문",
        "priceSnapshot": 49000,
        "status": "PAID",
        "refundedAt": null
      },
      {
        "orderItemId": 502,
        "courseId": 4,
        "courseTitleSnapshot": "React 18 완전 정복",
        "priceSnapshot": 59000,
        "status": "PAID",
        "refundedAt": null
      }
    ],
    "payments": [
      { "paymentId": 201, "amount": 108000, "method": "MOCK_CARD", "status": "SUCCESS", "paidAt": "2026-04-20T09:21:00" }
    ],
    "refunds": []
  }
}
```

**에러**: 다른 유저의 주문 → 404

### 3-B-5. POST `/api/orders/{id}/refund`

**요청 (전체 환불)**: 본문 생략 또는
```json
{ "reason": "USER_REQUEST" }
```

**요청 (부분 환불, 특정 강의만)**
```json
{ "reason": "USER_REQUEST", "orderItemIds": [502] }
```

**동작**:
1. 소유자 검증
2. 주문이 `PAID` 또는 `PARTIAL_REFUNDED` 상태인지 확인
3. 지정된 `order_items` (또는 미지정 시 PAID 상태 전부)에 대해
   - `orderItems.status=REFUNDED`, `refunded_at=now()`
   - `refunds` insert (모의, 항상 `SUCCESS`)
   - 해당 강의의 `enrollments` 삭제 + `lecture_progress` CASCADE (과제 범위 내에선 단순 삭제 허용)
4. 모든 item이 REFUNDED면 `orders.status=REFUNDED`, 일부면 `PARTIAL_REFUNDED`
5. `orders.refunded_amount` 증가

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "message": "환불 처리되었습니다.",
  "data": {
    "orderId": 77,
    "status": "PARTIAL_REFUNDED",
    "refundedAmount": 59000,
    "refundIds": [ 91 ]
  }
}
```

**에러**
- 404 `ORDER_NOT_FOUND`
- 409 `ORDER_NOT_REFUNDABLE` — 이미 전체 환불됨 / 미결제 상태
- 400 `INVALID_REFUND_ITEMS` — 요청한 orderItemIds가 주문에 속하지 않음

### 3-B-6. POST `/api/instructor/courses/{id}/cancel`

**권한**: `INSTRUCTOR` + 소유자

**동작**: 해당 강의를 사는 모든 PAID `order_items`를 찾아 `reason=COURSE_CANCELLED`로 일괄 환불. 강의는 `ARCHIVED` 전환.

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "courseId": 4,
    "archivedAt": "2026-04-21T12:00:00",
    "refundedOrderCount": 17,
    "refundedAmount": 1003000
  }
}
```

---

## 3-C. 리뷰 작성 게이트 (♻)

### 3-C-1. POST `/api/reviews`

**기존 P1 동작 + 아래 추가 검증:**
1. 요청자가 `courseId`에 대한 `enrollments` 보유
2. **해당 수강의 진도율 ≥ 80%**
3. 이미 작성한 리뷰 없음 (1인 1리뷰)

**에러**
- 403 `REVIEW_NOT_ALLOWED_INSUFFICIENT_PROGRESS` (422 권장) — 진도 80% 미달
  ```json
  {
    "success": false,
    "status": 422,
    "message": "진도 80% 이상 수강한 경우에만 리뷰를 작성할 수 있습니다.",
    "data": { "currentProgressRate": 42.3, "requiredRate": 80.0 }
  }
  ```
- 409 `REVIEW_ALREADY_EXISTS`
- 403 `NOT_ENROLLED`

---

## 3-D. 이어듣기 (🆕)

### 3-D-1. PUT `/api/playback`

**요청**
```json
{
  "enrollmentId": 301,
  "lectureId": 100,
  "positionSeconds": 437
}
```

**동작**: `(enrollmentId, lectureId)` upsert. `last_played_at=now()`.

- 프론트는 재생 중 **10초 주기** 혹은 pause/이탈 시 호출
- 수강 소유자 아니면 403

**응답 — 200** (본문 없음 또는 단순 ack)

### 3-D-2. GET `/api/playback/lectures/{lectureId}?enrollmentId={id}`

**응답**
```json
{ "success": true, "status": 200, "data": { "positionSeconds": 437, "lastPlayedAt": "..." } }
```

### 3-D-3. GET `/api/playback/enrollments/{enrollmentId}/resume`

**동작**: 해당 수강의 `playback_positions` 중 `last_played_at` 최신 1건 + 그 렉처 기본 정보.

**응답 — 200**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "lecture": {
      "id": 100,
      "title": "스프링부트 프로젝트 생성",
      "sectionTitle": "1. 시작하기",
      "videoUrl": "...",
      "durationSeconds": 600
    },
    "positionSeconds": 437,
    "lastPlayedAt": "2026-04-20T09:21:00"
  }
}
```

**에러**: 404 — 이어볼 기록 없음 (= 처음 듣기 상태)

---

## 3-E. 북마크 (🆕)

### 3-E-1. POST `/api/bookmarks`

**요청**
```json
{
  "lectureId": 100,
  "positionSeconds": 437,
  "memo": "여기 면접 단골질문"
}
```

**응답 — 201**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 11,
    "lectureId": 100,
    "positionSeconds": 437,
    "memo": "여기 면접 단골질문",
    "createdAt": "2026-04-20T09:30:00"
  }
}
```

**권한**: 로그인 사용자. `lectureId`에 대한 enrollment가 없으면 403 `NOT_ENROLLED`.

### 3-E-2. GET `/api/bookmarks/lectures/{lectureId}`

해당 렉처에 **내가 찍은** 북마크 목록 (position 오름차순).

### 3-E-3. GET `/api/bookmarks/me`

내 전체 북마크를 lecture/course 정보와 함께 페이지네이션 반환. "내 북마크" 페이지용.

### 3-E-4. PUT `/api/bookmarks/{id}` · DELETE `/api/bookmarks/{id}`

- 소유자만. 다른 유저의 id면 **404** (은폐).

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
| `CartAddRequestDTO` 🆕 | `courseId` | `@NotNull @Positive` |
| `CheckoutRequestDTO` 🆕 | `orderId` | `@NotNull @Positive` |
| `RefundRequestDTO` 🆕 | `reason` | `@Pattern(USER_REQUEST\|COURSE_CANCELLED\|CAPACITY_EXCEEDED)` (nullable → default USER_REQUEST) |
| `RefundRequestDTO` 🆕 | `orderItemIds` | optional `List<Long>`, 각 원소 `@Positive` |
| `PlaybackUpsertRequestDTO` 🆕 | `enrollmentId` | `@NotNull @Positive` |
| `PlaybackUpsertRequestDTO` 🆕 | `lectureId` | `@NotNull @Positive` |
| `PlaybackUpsertRequestDTO` 🆕 | `positionSeconds` | `@Min(0)` |
| `BookmarkCreateRequestDTO` 🆕 | `lectureId` | `@NotNull @Positive` |
| `BookmarkCreateRequestDTO` 🆕 | `positionSeconds` | `@Min(0)` |
| `BookmarkCreateRequestDTO` 🆕 | `memo` | `@Size(max=500)` |

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
| Auth | 5 | 부분 | — | `/me`만 인증 필수 |
| Public (조회) | 5 | ❌ | — | `publish_status=PUBLISHED` 필터 |
| Student/공통 (P1 계열) | 8 | ✅ | — | 작성자/수강자 본인, 리뷰는 80% 게이트 |
| 장바구니 🆕 | 4 | ✅ | — | 소유자 본인 |
| 주문/결제 🆕 | 5 | ✅ | — | 소유자, 상태 머신 |
| 이어듣기 🆕 | 3 | ✅ | — | enrollment 소유자 |
| 북마크 🆕 | 5 | ✅ | — | 소유자 |
| Instructor | 18 | ✅ | `INSTRUCTOR` | **소유자 검증** |

총 **53개** 엔드포인트.
