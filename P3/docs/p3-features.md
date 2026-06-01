# P3 운영형 기능 명세 (DevLearn)

P3는 P2(강사 역할·모의 결제·이어듣기·리뷰 게이트) 위에 **운영형/외부연동/관측성** 기능과
2차에서 누락된 **관리자 기능**, 그리고 **강사–학생 Q&A 게시판**을 추가한다.

> 모든 응답은 P1/P2 공통 `GlobalApiResponse` 포맷. 권한 없는 리소스 접근은 **404(존재 은폐)**.
> 커밋·에러·UI 카피는 한국어. 하위호환: 기존 경로/필드 삭제·변경 없이 추가만.

---

## 1. 관리자(ADMIN) 역할

- `Role` enum에 `ADMIN` 추가 (STUDENT/INSTRUCTOR 하위호환 유지)
- `/api/admin/**` → `hasRole('ADMIN')` (SecurityConfig 3중 인가 + `@PreAuthorize`)
- **권한 상승 방지**: 회원가입으로 ADMIN 자가 부여 불가 (`403 ADMIN_SIGNUP_FORBIDDEN`)
- 관리자 계정은 `AdminAccountInitializer`가 부팅 시 `ADMIN_PASSWORD` 설정 시에만 생성
- `User.active` 컬럼: 비활성 계정은 JWT 인증 컨텍스트가 설정되지 않아 즉시 차단

### 관리자 API

| 영역 | 메서드·경로 | 설명 |
|------|------------|------|
| 사용자 | `GET /api/admin/users?role&keyword&page` | 목록(역할·키워드·페이징) |
| | `GET /api/admin/users/{id}` | 단건 |
| | `PATCH /api/admin/users/{id}/role` | 역할 변경(본인·ADMIN 대상 금지, INSTRUCTOR 승급 시 프로필 자동 생성) |
| | `POST /api/admin/users/{id}/deactivate` `/activate` | 계정 정지/활성화 |
| 강의 | `GET /api/admin/courses?status&keyword&page` | 전체 강의(DRAFT 포함) |
| | `POST /api/admin/courses/{id}/block` | 차단(ARCHIVED 전이 + 사유) |
| | `POST /api/admin/courses/{id}/unblock` | 차단 해제(DRAFT 복귀) |
| 주문/매출 | `GET /api/admin/orders?status&page` | 전체 주문(주문자명 포함) |
| | `GET /api/admin/orders/sales-summary` | 매출 요약(gross/refunded/net, 상태별 건수) |
| | `POST /api/admin/orders/{id}/refund` | 강제 환불(수강 취소 포함) |
| 신고 | `GET /api/admin/reports?status&targetType&page` | 신고 목록 |
| | `POST /api/admin/reports/{id}/resolve` | 처리(대상 삭제 옵션) |
| | `POST /api/admin/reports/{id}/dismiss` | 반려 |

프론트: `/admin` 콘솔(대시보드·사용자·강의·주문/매출·신고 5개 탭).

---

## 2. 신고(Report) — 모더레이션

- `Report(reporterId, targetType[REVIEW|QNA_QUESTION|QNA_ANSWER], targetId, reason, status)`
- 상태머신: `PENDING → RESOLVED | DISMISSED`
- 신고자×대상 UNIQUE(중복 신고 `409`), 본인 콘텐츠 신고 금지
- `ReportTargetResolver` SPI + 레지스트리 — 도메인별 대상 조회/삭제 위임(리뷰·Q&A 구현)
- `POST /api/reports` (로그인 사용자), 처리/반려는 관리자 전용

---

## 3. Q&A 게시판 (강의 단위)

- `QnaQuestion`(courseId·authorId·title·content·isPrivate·answerCount), `QnaAnswer`(questionId·authorId·authorRole·content)
- **질문 작성**: 해당 강의 수강생만(`403 NOT_ENROLLED`)
- **답변**: 강의 강사 또는 관리자만(`403 NOT_ANSWERABLE`)
- 질문 수정: 작성자 / 삭제: 작성자·강사·관리자
- 비공개 질문은 작성자·강사·관리자만 열람(그 외 마스킹), IDOR 시 `404`

| 메서드·경로 | 설명 |
|------------|------|
| `GET /api/qna/courses/{courseId}/questions` | 강의 질문 목록(페이징) |
| `POST /api/qna/courses/{courseId}/questions` | 질문 작성(수강생) |
| `GET /api/qna/questions/{id}` | 상세 + 답변 |
| `PUT /api/qna/questions/{id}` · `DELETE` | 수정/삭제 |
| `POST /api/qna/questions/{id}/answers` | 답변 작성(강사·관리자) |
| `PUT /api/qna/answers/{id}` · `DELETE` | 답변 수정/삭제 |

프론트: 강의 상세 → `Q&A` 버튼 → `/courses/:id/qna`.

---

## 4. 알림 + 스케줄러 (아웃박스 패턴)

- `NotificationOutbox`: `dedupKey` UNIQUE로 **멱등 적재**, 지수 백오프 재시도(최대 5회)
- 채널: `DiscordWebhookSender`(RestClient, `DISCORD_WEBHOOK_URL`), `LogNotificationSender`(fallback)
- `NotificationService.enqueue`: `REQUIRES_NEW`로 비즈니스 트랜잭션과 분리(알림 실패가 본 흐름을 막지 않음)
- `NotificationDispatcher`: `@Scheduled` 배치 + 단건 독립 트랜잭션(`NotificationOutboxProcessor`)
- **이벤트**: 신규 수강·결제 / Q&A 새 질문·답변 / 신고 접수(관리자)
- 환경변수: `NOTIFICATION_CHANNEL`, `DISCORD_WEBHOOK_URL`, `NOTIFICATION_DISPATCH_INTERVAL_MS`

> 외부 API 다운 시: 발송 실패 row는 PENDING 유지 + 백오프 후 재시도, 5회 초과 시 FAILED 종결.

---

## 5. 관측성(모니터링)

- Spring Boot **Actuator**: `/actuator/health`·`/actuator/info` 공개, `metrics`·`prometheus`는 ADMIN 전용
- liveness/readiness probe 활성화
- `RequestLoggingFilter`: 요청마다 `requestId`(MDC) 부여 + `X-Request-Id` 응답 헤더 + 요청 1줄 구조화 로그
- 로그 패턴에 `reqId` 포함

---

## 6. 보안 하드닝

- **XSS**: `HtmlSanitizer`로 저장 전 정제(script·이벤트핸들러·js스킴 제거, 꺾쇠 이스케이프) — Q&A·리뷰·신고 사유. 프론트(React)와 함께 다층 방어
- **SQLi**: 전 쿼리 JPA 파라미터 바인딩(문자열 연결 없음)
- **보안 응답 헤더**: `X-Content-Type-Options=nosniff`, `X-Frame-Options=DENY`, `Referrer-Policy`, CSP
- **권한 상승 방지**: ADMIN 자가 회원가입 차단, 관리자 대상 역할 변경/정지 금지

---

## 7. 성능

- 신규 테이블 인덱스: `qna_questions(course_id, author_id)`, `qna_answers(question_id)`,
  `notification_outbox(status)`, `reports(reporter_id,target_type,target_id UNIQUE)`
- 기존 핫패스 인덱스 보강: `reviews(course_id, user_id)`, `enrollments(course_id)`
- 운영 기본 `spring.jpa.show-sql=false`(환경변수 토글)
- 목록 API 페이징(관리자 사용자/강의/주문/신고, Q&A)
