# 04. API 설계 — DevLearn P2

모든 응답은 `GlobalApiResponse<T>` 래핑:
```json
{ "success": true, "status": 200, "data": ... }
{ "success": false, "status": 4xx, "error": { "code": "...", "message": "..." } }
```

`Authorization: Bearer <accessToken>` 헤더로 인증. 인증 필요 표시: 🔒(학생/강사 무관), 👨‍🏫(INSTRUCTOR 전용), 🌐(공개).

## 1. Auth — `/api/auth`

| Method | Path | 권한 | 동작 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 🌐 | 회원가입 (req: `{username, password, role?}`. role 미지정 시 STUDENT). HTTP 201. |
| POST | `/api/auth/login` | 🌐 | 로그인 → `{accessToken, refreshToken}` |
| POST | `/api/auth/refresh` | 🌐 | 리프레시 → 새 access/refresh |
| POST | `/api/auth/logout` | 🔒 | 리프레시 토큰 무효화 |
| GET | `/api/auth/me` | 🔒 | `{userId, username, role}` |

**MeResponseDTO 예:**
```json
{ "userId": 7, "username": "alice", "role": "STUDENT" }
```

## 2. 카테고리 — `/api/categories`

| Method | Path | 권한 | 동작 |
|--------|------|------|------|
| GET | `/api/categories` | 🌐 | 전체 카테고리 |
| POST | `/api/categories` | 🔒 | 생성 (관리자 분리 없음, 학습용) |
| PUT | `/api/categories/{id}` | 🔒 | 수정 |
| DELETE | `/api/categories/{id}` | 🔒 | 삭제 |

## 3. 강의 공개 조회 — `/api/courses`

| Method | Path | 권한 | 동작 |
|--------|------|------|------|
| GET | `/api/courses?categoryId=&difficulty=&keyword=&page=&size=` | 🌐 | **PUBLISHED**만 페이징 반환. DRAFT/ARCHIVED 제외. |
| GET | `/api/courses/{id}` | 🌐 | 상세 (PUBLISHED만, 아니면 `404 COURSE_NOT_FOUND`) |

응답 DTO: `CoursePageResponseDTO`, `CourseDetailResponseDTO` (section/lecture 포함).

## 4. 강사 콘솔 — `/api/instructor/**` 👨‍🏫

클래스 레벨 `@PreAuthorize("hasRole('INSTRUCTOR')")` + 모든 메서드 시작점에서 `OwnershipValidator.requireOwnedCourse(courseId, principal.userId)`.

### 4-1. 강의 CRUD — `/api/instructor/courses`

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/instructor/courses` | 강의 생성 (DRAFT 상태로) |
| GET | `/api/instructor/courses?status=DRAFT\|PUBLISHED\|ARCHIVED` | 내 강의 목록 |
| GET | `/api/instructor/courses/{id}` | 내 강의 상세 |
| PUT | `/api/instructor/courses/{id}` | 수정 |
| DELETE | `/api/instructor/courses/{id}` | 삭제 (soft delete) |
| POST | `/api/instructor/courses/{id}/publish` | DRAFT → PUBLISHED. 발행 조건 미달 시 `422 PUBLISH_VALIDATION_FAILED` |
| POST | `/api/instructor/courses/{id}/archive` | PUBLISHED → ARCHIVED |
| POST | `/api/instructor/courses/{id}/cancel` | **폐강**: 전체 수강생 환불 + ARCHIVED. 트랜잭션 |

**InstructorCourseCreateRequest:**
```json
{
  "title": "Spring Boot 입문",
  "description": "...",
  "categoryId": 1,
  "difficulty": "초급",
  "price": 49000
}
```

### 4-2. 섹션 — `/api/instructor/sections`

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/instructor/sections` | `{courseId, title, orderNum}` |
| PUT | `/api/instructor/sections/{id}` | 수정 |
| DELETE | `/api/instructor/sections/{id}` | 삭제 |

### 4-3. 강의차시 — `/api/instructor/lectures`

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/instructor/lectures` | `{sectionId, title, videoUrl, orderNum, durationSeconds}` |
| PUT | `/api/instructor/lectures/{id}` | 수정 |
| DELETE | `/api/instructor/lectures/{id}` | 삭제 |

### 4-4. 프로필 (강사 자신) — `/api/instructor/profile`

| Method | Path | 동작 |
|--------|------|------|
| GET | `/api/instructor/profile` | 내 프로필 (없으면 빈 골격 응답 또는 404, 학습용은 빈 골격 권장) |
| PUT | `/api/instructor/profile` | `{displayName, bio, careerYears, profileImageUrl}` 업서트 |

### 4-5. 대시보드 — `/api/instructor/dashboard`

| Method | Path | 동작 |
|--------|------|------|
| GET | `/api/instructor/dashboard` | 통계 응답 |

**InstructorDashboardResponse 필드:**
```json
{
  "totalCourses": 5,
  "publishedCourses": 3,
  "totalStudents": 142,
  "recentEnrollments": [
    { "courseTitle": "...", "username": "alice", "enrolledAt": "..." }
  ]
}
```

### 4-6. 수강생 목록 — `/api/instructor/courses/{id}/students`

`GET` — 해당 강의 수강생 리스트(username, enrolledAt, 진도율). 소유자 아니면 `404`.

## 5. 공개 강사 프로필 — `/api/instructors/{userId}` 🌐

| Method | Path | 동작 |
|--------|------|------|
| GET | `/api/instructors/{userId}` | 강사 공개 정보 + 해당 강사의 PUBLISHED 강의 목록 |

**InstructorPublicProfileResponse:**
```json
{
  "userId": 2,
  "username": "tom",
  "displayName": "톰 강사",
  "bio": "10년차 백엔드 개발자",
  "careerYears": 10,
  "profileImageUrl": "...",
  "courses": [ { "id": 5, "title": "...", "price": 49000, ... } ]
}
```

## 6. 무료 수강 — `/api/enrollments` 🔒

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/enrollments` | `{courseId}`. **course.price > 0이면 `400 COURSE_NOT_FREE`**. 중복은 `409 ALREADY_ENROLLED` |
| GET | `/api/enrollments/me` | 내 수강 목록 |
| GET | `/api/enrollments/{id}/progress-rate` | 진도율 (0.0~100.0 float). 본인 enrollment 아니면 `404 ENROLLMENT_NOT_FOUND` |
| DELETE | `/api/enrollments/{id}` | 본인 수강 취소 (학습용 단순 hard delete) |

## 7. 진도 — `/api/progress` 🔒

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/progress/complete` | `{enrollmentId, lectureId}` 완료 표시. enrollment 소유 검증 |

## 8. 리뷰 — `/api/reviews`

| Method | Path | 권한 | 동작 |
|--------|------|------|------|
| POST | `/api/reviews` | 🔒 | `{courseId, rating(1~5), comment}`. 진도율 < 80% 면 `422 REVIEW_PROGRESS_GATE` |
| GET | `/api/reviews/courses/{courseId}` | 🌐 | 강의별 리뷰 목록 |
| DELETE | `/api/reviews/{id}` | 🔒 | 본인 리뷰 삭제 |

**REVIEW_PROGRESS_GATE 응답 body:**
```json
{
  "success": false,
  "status": 422,
  "error": {
    "code": "REVIEW_PROGRESS_GATE",
    "message": "진도율 80% 이상 달성 후 리뷰를 작성할 수 있습니다.",
    "currentProgressRate": 64.3,
    "requiredRate": 80.0
  }
}
```

## 9. 장바구니 — `/api/cart` 🔒

| Method | Path | 동작 |
|--------|------|------|
| GET | `/api/cart` | 내 장바구니 (course 정보 + 합계) |
| POST | `/api/cart/items` | `{courseId}` 담기. 중복은 `409 CART_DUPLICATE`. 이미 수강 중인 강의는 `409 ALREADY_ENROLLED`. 무료 강의 담기는 `409 COURSE_NOT_PURCHASABLE` |
| DELETE | `/api/cart/items/{courseId}` | 단건 제거 |
| DELETE | `/api/cart` | 전체 비우기 |

## 10. 주문 — `/api/orders` 🔒

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/orders` | **장바구니 전체를 스냅샷**해 주문(PENDING) 생성. 빈 장바구니는 `400 EMPTY_CART`. 가격 정합성 깨졌으면 `422 CART_SNAPSHOT_INVALID` |
| GET | `/api/orders?status=` | 내 주문 목록 |
| GET | `/api/orders/{id}` | 내 주문 상세. 타인 주문은 `404 ORDER_NOT_FOUND` |

**OrderResponse:**
```json
{
  "id": 11,
  "orderNo": "20260326-0007",
  "status": "PAID",
  "totalAmount": 98000,
  "refundedAmount": 0,
  "paidAt": "2026-03-26T15:21:03",
  "items": [
    { "id": 21, "courseId": 5, "courseTitleSnapshot": "Spring Boot", "priceSnapshot": 49000, "status": "ACTIVE" }
  ]
}
```

## 11. 결제 — `/api/payments` 🔒

| Method | Path | 동작 |
|--------|------|------|
| POST | `/api/payments/checkout` | **요청 body: `{ orderId, simulateFailure? }`만**. 금액은 서버가 `orders.total_amount`에서 재계산. PENDING 아니면 `409 ORDER_NOT_PAYABLE`. `MockPaymentGateway` 호출 → SUCCESS 시 PAID 전이 + enrollment 자동 생성 + cart 비우기 (트랜잭션) |
| POST | `/api/payments/refund/{orderId}` | `{reason?, orderItemIds?}`. orderItemIds 비우면 전체 환불. enrollment hard delete + refund 레코드 + order 상태 전이 |

`simulateFailure: true` 옵션은 학습용 — Mock 게이트웨이가 강제로 FAILED 반환.

## 12. 이어듣기 — `/api/playback` 🔒

| Method | Path | 동작 |
|--------|------|------|
| PUT | `/api/playback` | `{enrollmentId, lectureId, currentTimeSeconds}` 업서트. enrollment 소유 검증 → `404 ENROLLMENT_NOT_FOUND` |
| GET | `/api/playback/lectures/{lectureId}?enrollmentId=` | 특정 강의차시 재생 위치 |
| GET | `/api/playback/enrollments/{enrollmentId}/resume` | 가장 최근 시청 강의차시 + 위치 (없으면 첫 강의차시·0초) |

## 13. 북마크 — `/api/bookmarks` 🔒

| Method | Path | 동작 |
|--------|------|------|
| GET | `/api/bookmarks?lectureId=` | 내 북마크. lectureId 미지정 시 전체 |
| POST | `/api/bookmarks` | `{lectureId, timeSeconds, memo?}` |
| PUT | `/api/bookmarks/{id}` | 본인 북마크 메모/시간 수정 |
| DELETE | `/api/bookmarks/{id}` | 본인 북마크 삭제 |

## 14. ErrorCode 카탈로그

| 코드 | HTTP | 메시지 (기본) |
|------|------|--------------|
| `ACCESS_DENIED` | 403 | 해당 리소스에 접근할 권한이 없습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `COURSE_NOT_FOUND` | 404 | 강의를 찾을 수 없습니다. (소유자 아닌 강사 접근 포함) |
| `PUBLISH_VALIDATION_FAILED` | 422 | 발행 조건을 충족하지 못했습니다. |
| `ALREADY_PUBLISHED` | 409 | 이미 발행된 강의입니다. |
| `ALREADY_ARCHIVED` | 409 | 이미 아카이브된 강의입니다. |
| `INVALID_STATUS_TRANSITION` | 409 | 허용되지 않은 상태 전이입니다. |
| `SECTION_NOT_FOUND` | 404 | 섹션을 찾을 수 없습니다. |
| `LECTURE_NOT_FOUND` | 404 | 강의차시를 찾을 수 없습니다. |
| `PROFILE_NOT_FOUND` | 404 | 강사 프로필을 찾을 수 없습니다. |
| `COURSE_NOT_FREE` | 400 | 유료 강의는 결제 후 수강할 수 있습니다. |
| `ALREADY_ENROLLED` | 409 | 이미 수강 중인 강의입니다. |
| `ENROLLMENT_NOT_FOUND` | 404 | 수강 정보를 찾을 수 없습니다. |
| `CART_DUPLICATE` | 409 | 이미 장바구니에 담긴 강의입니다. |
| `EMPTY_CART` | 400 | 장바구니가 비어 있습니다. |
| `COURSE_NOT_PURCHASABLE` | 409 | 결제할 수 없는 강의입니다. |
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없습니다. |
| `ORDER_NOT_PAYABLE` | 409 | 결제할 수 없는 주문 상태입니다. |
| `ORDER_NOT_REFUNDABLE` | 409 | 환불할 수 없는 주문 상태입니다. |
| `INVALID_REFUND_ITEMS` | 400 | 환불 대상 항목이 올바르지 않습니다. |
| `CART_SNAPSHOT_INVALID` | 422 | 장바구니 가격 정보가 변경되었습니다. |
| `PAYMENT_FAILED` | 402 | 결제에 실패했습니다. |
| `PAYMENT_NOT_FOUND` | 404 | 결제 정보를 찾을 수 없습니다. |
| `REVIEW_PROGRESS_GATE` | 422 | 진도율 80% 이상 달성 후 리뷰를 작성할 수 있습니다. |
| `REVIEW_NOT_FOUND` | 404 | 리뷰를 찾을 수 없습니다. |
| `NOT_ENROLLED` | 403 | 수강 중인 강의에만 리뷰를 작성할 수 있습니다. |

## 15. CORS

`app.cors.allowed-origins` 환경변수로 콤마 구분 등록. 기본 dev: `http://localhost:5173`.

## 16. Swagger

`/swagger-ui/**`, `/v3/api-docs/**`. 운영에서는 `SWAGGER_ENABLED=false`로 비활성.
