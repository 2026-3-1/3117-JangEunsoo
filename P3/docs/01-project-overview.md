# 01. 프로젝트 개요 — DevLearn P2

## 1. 비전

**DevLearn P2**는 학생이 강의를 듣고, 강사가 강의를 올리는 **풀스택 인강 플랫폼**의 학습용 구현체다. 2026년 3학년 1학기 프로젝트 실습 수업 결과물.

P1(JWT 인증·강의 목록·수강·진도·리뷰)에 다음을 더해 P2가 된다:

- **강사 역할 도입** — User에 `role` 컬럼을 두고 INSTRUCTOR/STUDENT를 구분
- **강사 콘솔** — 강의 CRUD·발행·폐강·공개 프로필·대시보드
- **유료 강의 + 모의 결제** — 장바구니 → 주문 → 모의 PG → enrollment 자동 생성 → 환불
- **이어듣기·북마크** — 학습 UX 보강
- **80% 진도 리뷰 게이트** — 진도가 80% 이상일 때만 리뷰 작성 가능

> 결제는 실제 PG와 연동하지 않는다. `MockPaymentGateway`는 항상 `SUCCESS`를 반환하며, `orders`·`payments` 레코드를 통해 결제 흐름을 시뮬레이션할 뿐이다.

## 2. 대상 사용자

| 역할 | 설명 | 예시 동작 |
|------|------|-----------|
| 학생 (STUDENT) | 강의 탐색·수강·결제·리뷰 작성 | 장바구니에 담아 결제 → 강의 시청 → 80% 듣고 리뷰 작성 |
| 강사 (INSTRUCTOR) | 강의 CRUD·발행·수강생 관리 | DRAFT 강의 작성 → PUBLISHED 전환 → 폐강 시 일괄 환불 |

> 강사도 다른 강사의 강의를 수강하고 리뷰할 수 있다(역할 무관). 단 자기 강의에는 리뷰를 작성하지 못하는 별도 정책은 없다 — 학습용이라 단순화.

## 3. 핵심 기능 7가지

1. **JWT 인증 + 역할 클레임** — `Authorization: Bearer <jwt>` 헤더, JWT의 `role` 클레임으로 인가
2. **강사 콘솔** — `/instructor/**` 경로는 INSTRUCTOR 전용. 강의/섹션/강의차시 CRUD, 발행/아카이브/폐강
3. **공개 강사 프로필** — `/instructors/:userId` 페이지에 강사 소개·강의 목록
4. **장바구니 → 주문 → 모의 결제** — `MockPaymentGateway`가 SUCCESS 반환, OrderItem.priceSnapshot으로 가격 고정
5. **환불 (전체/부분)** — 환불 시 enrollment hard delete, refund 레코드 생성, order 상태 전이
6. **이어듣기 + 북마크** — `PlaybackPosition` 업서트, 강의차시별 시간+메모 북마크
7. **80% 진도 리뷰 게이트** — 서버가 진도율 재계산 후 미달 시 `422 REVIEW_PROGRESS_GATE`

## 4. P1 → P2 변경 요약

| 영역 | P1 | P2 |
|------|-----|-----|
| `users.role` | 없음 | `STUDENT` / `INSTRUCTOR` (default STUDENT) |
| 강의 소유 | `instructor_name` 문자열 | `instructor_id` FK (+ `instructor_name` deprecated) |
| 강의 발행 | 즉시 노출 | `DRAFT` / `PUBLISHED` / `ARCHIVED` |
| 강의 가격 | 없음 | `price` BIGINT (0=무료) |
| 유료 수강 | 없음 | 장바구니→주문→모의 결제 경로 |
| 무료 수강 | `POST /api/enrollments` | 동일. 단 유료에 호출하면 `400 COURSE_NOT_FREE` |
| 리뷰 작성 | 제한 없음 | 진도 ≥ 80% (서버 재계산) |
| 이어듣기 | 없음 | `PUT /api/playback` (upsert) |
| 북마크 | 없음 | `/api/bookmarks` |
| 강사 프로필 | 없음 | `instructor_profiles` 테이블 + 공개 API |

## 5. Non-Goals (이 프로젝트에서 하지 않는 것)

- 실제 PG 연동 (PortOne·KG이니시스 등)
- 실시간 알림·웹훅·이메일 전송
- 동영상 인코딩·CDN·DRM (강의 `video_url`은 외부 링크 가정)
- 검색 엔진(ES/OpenSearch). MySQL LIKE 검색으로 충분
- 사용자 프로필 사진 업로드 — `profile_image_url`은 외부 URL 문자열만 저장
- 다국어 i18n — 한국어 단일

## 6. 동작 시나리오 (행복 경로)

```
[학생 A]
  signup → login → /courses 탐색 → 강의 상세 → 장바구니 → 주문 생성
  → 체크아웃(모의 결제 SUCCESS) → enrollment 자동 생성
  → 강의 시청 (PlaybackPosition 주기 업서트) → 북마크 추가
  → 80% 진도 달성 → 리뷰 작성 성공

[강사 B]
  signup(role=INSTRUCTOR) → /instructor/dashboard 통계 확인
  → /instructor/courses/new → DRAFT 강의 작성 → 섹션·강의차시 추가
  → /publish → PUBLISHED 전환 → 학생 A가 수강
  → /cancel → 폐강 시 모든 학생에게 전액 환불 + ARCHIVED 전환
```

## 7. 성공 기준 (DoD)

1. P1의 모든 기능이 깨지지 않고 동작 (회귀 테스트 통과)
2. STUDENT 계정으로 `/api/instructor/**` 호출 시 `403 ACCESS_DENIED`
3. 타인 주문/북마크/재생 위치 접근 시 `404` (존재 은폐)
4. `MockPaymentGateway`로 결제 완료 시 enrollment 자동 생성
5. 진도 79.9% 상태에서 리뷰 작성 시 `422 REVIEW_PROGRESS_GATE`
6. 강사 폐강 시 모든 학생 환불 + ARCHIVED 전환이 하나의 트랜잭션
7. `docker compose up`만으로 mysql + backend + frontend가 기동
