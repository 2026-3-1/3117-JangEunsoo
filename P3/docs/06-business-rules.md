# 06. 비즈니스 규칙 — DevLearn P2

각 규칙은 코드를 작성할 때 **반드시 따라야 하는 동작 명세**다. 헷갈리면 이 문서를 진실원으로 본다.

## 1. 강의 발행 상태 머신

```
        ┌────────┐  publish()   ┌───────────┐  archive()   ┌──────────┐
delete  │ DRAFT  │ ───────────► │ PUBLISHED │ ───────────► │ ARCHIVED │
 (soft) │        │              │           │   cancel()   │          │
        └────────┘              └───────────┘ ───────────► └──────────┘
            ▲                          │
            └──────── (불가) ──────────┘
```

| 전이 | 허용 | 실패 시 코드 |
|------|------|-------------|
| DRAFT → PUBLISHED | 모든 필드 충족 시 | 미달 → `422 PUBLISH_VALIDATION_FAILED` |
| PUBLISHED → PUBLISHED | 호출 자체가 무효 | `409 ALREADY_PUBLISHED` |
| PUBLISHED → ARCHIVED | OK | — |
| ARCHIVED → PUBLISHED | 불가 | `409 INVALID_STATUS_TRANSITION` |
| ARCHIVED → DRAFT | 불가 | `409 INVALID_STATUS_TRANSITION` |

### 발행 조건 (PUBLISH_VALIDATION_FAILED 검증)

다음 중 하나라도 실패하면 발행 거부:

- `title` 비어있지 않음
- `description` 비어있지 않음
- `categoryId` 지정
- `price >= 0`
- 최소 1개의 `Section` 보유
- 각 Section에 최소 1개의 `Lecture` 보유

### 폐강 (`/cancel`)

- 현재 PUBLISHED만 폐강 가능
- 트랜잭션 내에서:
  1. 모든 active enrollment 조회 → 해당 student의 `orders.order_items`를 찾아 `priceSnapshot` 만큼 환불 처리
  2. `refunds` 레코드 생성 (`reason = COURSE_CANCELLED`)
  3. enrollment hard delete (lecture_progress·playback_position cascade)
  4. `order_items.status = REFUNDED`, `orders.refunded_amount` 갱신, 필요 시 `orders.status` 전이
  5. `courses.publish_status = ARCHIVED`

## 2. 강의 가격 & 수강 경로

| course.price | 수강 경로 |
|--------------|-----------|
| `0` (무료) | `POST /api/enrollments` (직접). 결제·장바구니 거치지 않음 |
| `> 0` (유료) | 장바구니 → 주문 → 결제 SUCCESS 후 **자동 enrollment 생성** |

- 유료 강의에 `POST /api/enrollments` 호출 시 → `400 COURSE_NOT_FREE`
- 무료 강의를 장바구니에 담으려 하면 → `409 COURSE_NOT_PURCHASABLE`
- 이미 enrollment가 있는 강의를 장바구니에 담으려 하면 → `409 ALREADY_ENROLLED`

## 3. 장바구니 규칙

- 동일 강의 중복 담기 → `409 CART_DUPLICATE`
- 빈 장바구니로 주문 생성 시 → `400 EMPTY_CART`
- 강사 본인이 본인 강의를 담는 것에 대한 별도 차단은 없음 (학습용 단순화)
- 장바구니는 결제 SUCCESS 시 **전체 비워짐**

## 4. 주문 상태 머신

```
                       checkout(SUCCESS)        refund (전체)
       ┌──────┐  ────────────────────► ┌───────┐ ──────────────► ┌──────────┐
       │PENDING│                       │ PAID  │                  │ REFUNDED │
       └──────┘                        └───┬───┘                  └──────────┘
           │                               │ refund (일부)
           │ (만료/취소)                    ▼
           ▼                       ┌──────────────────┐
       ┌──────────┐                │ PARTIAL_REFUNDED │
       │ CANCELLED│                └──────────────────┘
       └──────────┘
```

| 전이 | 조건 | 실패 코드 |
|------|------|-----------|
| PENDING → PAID | 결제 SUCCESS | — |
| PENDING → CANCELLED | (학습용, 명시 API는 없지만 가능) | — |
| PAID → REFUNDED | 모든 OrderItem 환불 | — |
| PAID → PARTIAL_REFUNDED | 일부 OrderItem 환불 | — |
| 그 외 상태에서 checkout | | `409 ORDER_NOT_PAYABLE` |
| PENDING 상태에서 refund | | `409 ORDER_NOT_REFUNDABLE` |
| 이미 환불된 OrderItem 재환불 | | `409 ORDER_NOT_REFUNDABLE` |

## 5. 결제 (`/api/payments/checkout`)

### 요청 body 계약 (CRITICAL)

```json
{ "orderId": 123, "simulateFailure": false }
```

- **amount/price는 받지 않는다**. 서버가 `orders.total_amount`로 재계산
- `simulateFailure=true` 시 Mock 게이트웨이가 강제로 FAILED 반환

### checkout 처리 단계 (단일 트랜잭션)

1. `findByIdAndUserId(orderId, principal.userId)` 으로 주문 로드 → 없으면 `404 ORDER_NOT_FOUND`
2. `order.status == PENDING` 검증 → 아니면 `409 ORDER_NOT_PAYABLE`
3. `OrderItem.price_snapshot` 합 = `order.total_amount` 검증 → 깨졌으면 `422 CART_SNAPSHOT_INVALID`
4. `MockPaymentGateway.charge(order.total_amount, simulateFailure)` 호출
5. SUCCESS 시:
   - `payments` 레코드 INSERT (`status=SUCCESS`, `method=MOCK_CARD`, `mock_transaction_id=UUID`)
   - `order.markPaid()` → `status=PAID`, `paid_at=now()`
   - 각 OrderItem의 `course_id`에 대해 **enrollment INSERT** (UNIQUE 충돌 시 무시 — 이미 있으면 skip)
   - 해당 user의 `cart_items` 전체 delete
6. FAILED 시:
   - `payments` 레코드 INSERT (`status=FAILED`)
   - 주문 상태 변경 없음
   - `402 PAYMENT_FAILED` 응답

## 6. 환불 (`/api/payments/refund/{orderId}`)

### 요청 body

```json
{ "reason": "USER_REQUEST", "orderItemIds": [21, 22] }
```

- `orderItemIds` 미지정/빈 배열 → **전체 환불**
- `reason` 미지정 → `USER_REQUEST` 기본

### 처리 단계 (단일 트랜잭션)

1. `OwnershipValidator.requireOwnedOrder(orderId, userId)`
2. `order.status == PAID || PARTIAL_REFUNDED` 검증 → 아니면 `409 ORDER_NOT_REFUNDABLE`
3. 환불 대상 OrderItem 결정:
   - 미지정 시: `status=ACTIVE`인 모든 항목
   - 지정 시: 해당 id가 이 주문 소속이며 `ACTIVE`여야 함 → 위반 시 `400 INVALID_REFUND_ITEMS`
4. 각 대상 항목에 대해:
   - `refunds` 레코드 INSERT (`amount=item.priceSnapshot`, `order_item_id=item.id`, `reason`)
   - `item.status = REFUNDED`
   - 해당 student의 `enrollments` 행 hard delete (course_id 일치)
5. `order.refunded_amount += sum(refund.amount)`
6. 남은 ACTIVE item이 있으면 `order.status = PARTIAL_REFUNDED`, 없으면 `REFUNDED`

> 폐강(`/cancel`)도 내부적으로 동일 로직을 호출하되 `reason=COURSE_CANCELLED`.

## 7. 진도율 계산

```
progressRate = (완료한 lecture 수 / 해당 course의 전체 lecture 수) × 100
```

- **항상 서버에서 재계산**. 클라이언트가 보낸 값은 무시
- 분모가 0(강의에 lecture가 없음)이면 → `0.0`
- 응답 타입: `float`, 소수점 1자리 권장 (반올림)
- `GET /api/enrollments/{id}/progress-rate` → `{ "rate": 64.3 }`

## 8. 리뷰 80% 게이트

```
if (progressRate < 80.0) {
  throw new ReviewProgressGateException(progressRate, 80.0);
}
```

- 응답 body:
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
- `NOT_ENROLLED` (수강 안 함) 검증이 먼저, 그 다음 게이트
- 리뷰 수정 API는 만들지 않음 (학습용). 작성/삭제만

## 9. 이어듣기 (PlaybackPosition)

- 키: `UNIQUE(enrollment_id, lecture_id)` — 업서트 패턴
- 호출 주기: 클라이언트가 비디오 재생 중 5~10초마다 PUT
- `currentTimeSeconds < 0` → `400` (검증)
- enrollment 소유자 아니면 → `404 ENROLLMENT_NOT_FOUND`
- Resume 엔드포인트는 `MAX(updated_at)` 기준 가장 최근 강의차시 반환. 기록 없으면 첫 lecture·0초

## 10. 북마크

- lecture 단위. 강의(course) 전체에 대한 북마크가 아니라 **강의차시 + 초** 단위
- 검증: `lectureId`가 user의 어떤 enrollment에도 소속되지 않으면 → `404 ENROLLMENT_NOT_FOUND` (또는 LECTURE_NOT_FOUND 중 일관성 유지)
- 메모는 옵션 (NULL 허용)
- 동일 lecture·동일 time 다중 북마크 허용

## 11. 스냅샷 보존 원칙

| 데이터 | 시점 고정 | 이유 |
|--------|----------|------|
| `order_items.price_snapshot` | 주문 생성 시점 | 강의 가격이 나중에 바뀌어도 환불·정산 정확성 |
| `order_items.course_title_snapshot` | 주문 생성 시점 | 강의 제목 변경/삭제 후에도 주문 내역 표시 |
| `orders.order_no` | 주문 생성 시점 | `YYYYMMDD-####` 포맷, 유저에게 노출 |

## 12. 데이터 정합성 트랜잭션 경계

다음 동작은 **반드시 단일 트랜잭션** 안에서 수행:

| 동작 | 포함 작업 |
|------|-----------|
| 주문 생성 | order INSERT + order_items INSERT N건 |
| 결제 SUCCESS | payment INSERT + order.markPaid + N건 enrollment INSERT + cart_items 전체 DELETE |
| 환불 | refund INSERT + order_item.status 변경 + order.refunded_amount/status 갱신 + enrollment DELETE |
| 폐강 | (위 환불 로직을 모든 학생에게) + course.publish_status = ARCHIVED |

`@Transactional` (또는 동등) 어노테이션 누락 금지.

## 13. 수강 가능 여부 검증 (요약 함수)

```
isPurchasable(course, user):
    return course.publish_status == PUBLISHED
        and course.price > 0
        and not deleted
        and not alreadyEnrolled(user, course)
        and not alreadyInCart(user, course)
```

`POST /api/cart/items`에서 위 조건이 모두 참일 때만 200. 각 조건 실패 시 그에 맞는 에러 코드 반환.

## 14. 동시성 시나리오

학습용이라 깊이 다루지 않으나 다음은 인지해 둔다:

- 같은 유저가 동시에 같은 강의를 두 번 cart에 담기 → DB UNIQUE 제약으로 한 건만 성공, 나머지는 `409`
- 같은 유저가 동시에 동일 주문 결제 두 번 → 두 번째는 `409 ORDER_NOT_PAYABLE`
- enrollment INSERT 중복 → UNIQUE 충돌 → 결제 후 자동 enrollment 생성 시 ON DUPLICATE/예외 처리로 idempotent하게 다룬다
