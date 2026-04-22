# 02. 데이터 모델 — DevLearn P2

## 1. DB 선택

P1과 동일하게 **MySQL 8.x** 유지. JPA(Hibernate)의 `spring.jpa.hibernate.ddl-auto=update`에 의해 엔티티 변경 시 자동 `ALTER TABLE`이 실행된다. 학습 환경이므로 **별도 마이그레이션 도구(Flyway/Liquibase)는 도입하지 않는다** — 그 대신 이 문서의 SQL 블록을 "참고용 변경 스크립트"로 남겨, 수동 실행 가능 상태로 둔다.

| 항목 | 값 |
|------|-----|
| DBMS | MySQL 8.x |
| ORM | Hibernate (Spring Data JPA) |
| 문자셋 | `utf8mb4` (이모지/한글) |
| DDL 전략 | `ddl-auto=update` (P1 유지) |
| 마이그레이션 도구 | **없음** (엔티티 변경 → 자동 반영 + 수동 seed) |

---

## 2. P1 ERD 스냅샷 (현재 상태)

```
┌──────────────────┐      ┌─────────────────────┐
│ users            │      │ refresh_tokens      │
├──────────────────┤  1:1 ├─────────────────────┤
│ id (PK)          │◄─────┤ user_id (FK, UNIQUE)│
│ username (UQ,15) │      │ token               │
│ password         │      │ expiry_date         │
│ created_at       │      │ created_at          │
│ updated_at       │      │ updated_at          │
└──────────────────┘      └─────────────────────┘
        │
        │ 1:N
        ▼
┌──────────────────┐      ┌──────────────────┐
│ enrollments      │──N:1─► courses          │
├──────────────────┤      ├──────────────────┤
│ id (PK)          │      │ id (PK)          │
│ user_id          │      │ category_id (FK) │──┐
│ course_id        │      │ title            │  │
│ created_at       │      │ description      │  │
│ UNIQUE(user,     │      │ difficulty       │  │
│        course)   │      │ instructor_name  │◄─── 문자열, FK 아님
└──────┬───────────┘      │ deleted_at       │  │
       │ 1:N              └──────┬───────────┘  │
       ▼                         │              │ N:1
┌──────────────────┐              │ 1:N         ▼
│ lecture_progress │              ▼        ┌──────────────┐
├──────────────────┤       ┌──────────────┐│ categories   │
│ id (PK)          │       │ sections     │├──────────────┤
│ enrollment_id    │       ├──────────────┤│ id (PK)      │
│ lecture_id       │       │ id (PK)      ││ name         │
│ is_completed     │       │ course_id    │└──────────────┘
│ UNIQUE(enr,lec)  │       │ title        │
└──────────────────┘       │ order_num    │
                           └──────┬───────┘
                                  │ 1:N
                                  ▼
                           ┌──────────────┐
                           │ lectures     │
                           ├──────────────┤
                           │ id (PK)      │
                           │ section_id   │
                           │ title        │
                           │ video_url    │
                           │ order_num    │
                           └──────────────┘

┌──────────────────┐
│ reviews          │
├──────────────────┤
│ id (PK)          │
│ user_id          │
│ course_id        │
│ rating (int)     │
│ comment (TEXT)   │
│ created_at       │
└──────────────────┘
```

**P1의 주요 제약:**
- `courses.instructor_name`이 **문자열** (FK 없음) → 누가 만든 강의인지 시스템적으로 알 수 없음
- `users`에 **role 컬럼 없음** → 모두 동일 권한
- `courses`에 **발행 상태 없음** → 생성과 동시에 노출

---

## 3. P2 변경 사항 (Diff)

### 3-1. `users` 테이블 — `role` 컬럼 추가

| 컬럼 | 타입 | 기본값 | 비고 |
|------|------|-------|------|
| `role` | `VARCHAR(20)` | `'STUDENT'` | enum: `STUDENT` \| `INSTRUCTOR` |

JPA 측:

```java
// P2: User.java 변경 예시
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.STUDENT;

public enum Role {
    STUDENT, INSTRUCTOR
}
```

### 3-2. `instructor_profiles` 테이블 — 신설

강사에게만 필요한 소개/경력 정보를 `users`에서 분리하여 1:1로 연결.

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | FK→`users.id`, UNIQUE, NOT NULL | 1:1 |
| `display_name` | `VARCHAR(50)` | NOT NULL | 공개 노출 이름 |
| `bio` | `TEXT` | | 자기소개 |
| `career_years` | `INT` | | 경력(년) |
| `profile_image_url` | `VARCHAR(500)` | | |
| `created_at` | `DATETIME` | NOT NULL | |
| `updated_at` | `DATETIME` | NOT NULL | |

### 3-3. `courses` 테이블 — `instructor_id` FK + `publish_status` + `price` 추가

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `instructor_id` | `BIGINT` | FK→`users.id`, NOT NULL (신규 강의 기준) — 하위호환 위해 일시적으로 NULL 허용 후 backfill 완료 시 NOT NULL |
| `publish_status` | `VARCHAR(20)` | `'DRAFT'` default. enum: `DRAFT`/`PUBLISHED`/`ARCHIVED` |
| `published_at` | `DATETIME` | nullable. 처음 발행된 시각 (analytics용) |
| `price` 🆕 | `BIGINT` | NOT NULL default 0. 원(KRW) 단위. 0 = 무료 |

**`instructor_name`은 한동안 유지.** P3로 넘어가기 전에 제거 후보. JPA 엔티티에선 `@Deprecated` 표시.

### 3-4. `lectures` 테이블 — `duration_seconds` 추가 🆕

이어듣기/재생 위치 계산 및 UI 표기에 필요.

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `duration_seconds` | `INT` | NOT NULL default 0. 렉처 영상 길이(초) |

### 3-5. 신규 테이블: 장바구니 · 주문 · 결제 · 환불 🆕

수강생 확장 기능(PDF)의 결제/거래 기록을 담당. **모의 결제** 전제. 외부 PG 연동은 없음.

#### 3-5-1. `cart_items` — 장바구니 항목

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | FK→`users.id`, NOT NULL | 소유자 |
| `course_id` | `BIGINT` | FK→`courses.id`, NOT NULL | |
| `created_at` | `DATETIME` | NOT NULL | |
| — | — | **UNIQUE(user_id, course_id)** | 중복 담기 방지 |

> 수량 컬럼이 없는 이유: 강의는 **1인 1회** 상품. 수량 개념이 필요 없음.

#### 3-5-2. `orders` — 주문(헤더)

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | FK→`users.id`, NOT NULL | 주문자 |
| `order_no` | `VARCHAR(30)` | UNIQUE, NOT NULL | 사용자 노출 주문번호 (예: `ORD-20260421-0001`) |
| `status` | `VARCHAR(20)` | NOT NULL | enum: `PENDING` / `PAID` / `CANCELLED` / `REFUNDED` / `PARTIAL_REFUNDED` |
| `total_amount` | `BIGINT` | NOT NULL | 주문 총액(원). 주문 시점 스냅샷 |
| `refunded_amount` | `BIGINT` | NOT NULL default 0 | 누적 환불액 |
| `created_at` | `DATETIME` | NOT NULL | |
| `paid_at` | `DATETIME` | nullable | 결제 성공 시각 |
| `cancelled_at` | `DATETIME` | nullable | 취소 시각 |

#### 3-5-3. `order_items` — 주문 항목 (강의별 스냅샷)

주문 시점의 가격을 **스냅샷**으로 저장해야 한다. 이후 강사가 가격을 바꿔도 거래 기록은 변하지 않는다.

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `order_id` | `BIGINT` | FK→`orders.id`, NOT NULL | |
| `course_id` | `BIGINT` | FK→`courses.id`, NOT NULL | |
| `course_title_snapshot` | `VARCHAR(255)` | NOT NULL | 주문 시점 강의명 |
| `price_snapshot` | `BIGINT` | NOT NULL | 주문 시점 가격(원) |
| `status` | `VARCHAR(20)` | NOT NULL default `'PAID'` | `PAID` / `REFUNDED` — 부분 환불 추적용 |
| `refunded_at` | `DATETIME` | nullable | |

#### 3-5-4. `payments` — 결제 이벤트 (모의)

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK | |
| `order_id` | `BIGINT` | FK→`orders.id`, NOT NULL | |
| `amount` | `BIGINT` | NOT NULL | 결제 금액 |
| `method` | `VARCHAR(20)` | NOT NULL | `MOCK_CARD` 고정 (P2). P3에서 실제 PG |
| `status` | `VARCHAR(20)` | NOT NULL | `SUCCESS` / `FAILED` |
| `mock_transaction_id` | `VARCHAR(50)` | NOT NULL | UUID로 생성한 가짜 트랜잭션 ID |
| `paid_at` | `DATETIME` | NOT NULL | |

#### 3-5-5. `refunds` — 환불 이벤트

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK | |
| `order_id` | `BIGINT` | FK→`orders.id`, NOT NULL | |
| `amount` | `BIGINT` | NOT NULL | 환불 금액 |
| `reason` | `VARCHAR(50)` | NOT NULL | `USER_REQUEST` / `COURSE_CANCELLED` / `CAPACITY_EXCEEDED` |
| `status` | `VARCHAR(20)` | NOT NULL | `SUCCESS` (모의이므로 실패 경로 없음) |
| `refunded_at` | `DATETIME` | NOT NULL | |

> PDF의 "부득이하게 인원수 초과" · "인원미달로 강의 취소" 요구를 수용하기 위해 `reason`에 별도 값을 두었다. 강사가 `/api/instructor/courses/{id}/cancel`을 호출하면 해당 강의에 대한 모든 PAID 주문건에 대해 `reason=COURSE_CANCELLED`로 일괄 `refunds` 레코드 생성.

### 3-6. 신규 테이블: 학습 UX (이어듣기·북마크) 🆕

#### 3-6-1. `playback_positions` — 이어듣기 (재생 위치)

각 수강자(=enrollment)가 각 렉처에서 **어디까지 봤는지** 초 단위로 저장.

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK | |
| `enrollment_id` | `BIGINT` | FK→`enrollments.id`, NOT NULL | |
| `lecture_id` | `BIGINT` | FK→`lectures.id`, NOT NULL | |
| `position_seconds` | `INT` | NOT NULL default 0 | 재생 위치(초) |
| `last_played_at` | `DATETIME` | NOT NULL | 최근 재생 시각 |
| — | — | **UNIQUE(enrollment_id, lecture_id)** | 렉처당 한 행, upsert |

> "최근 본 렉처"는 `enrollment_id`로 필터한 뒤 `last_played_at DESC LIMIT 1`로 계산한다. 별도 컬럼 없음.

#### 3-6-2. `bookmarks` — 북마크(책갈피)

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| `id` | `BIGINT` | PK | |
| `user_id` | `BIGINT` | FK→`users.id`, NOT NULL | |
| `lecture_id` | `BIGINT` | FK→`lectures.id`, NOT NULL | |
| `position_seconds` | `INT` | NOT NULL | 해당 시점(초) |
| `memo` | `VARCHAR(500)` | nullable | 메모(선택) |
| `created_at` | `DATETIME` | NOT NULL | |
| `updated_at` | `DATETIME` | NOT NULL | |

> 중복 방지 UNIQUE를 두지 않는다. 같은 렉처의 여러 위치에 북마크를 달 수 있어야 한다.

### 3-7. `reviews` 테이블 — 변경 없음, 단 **서비스 레벨 게이트** 추가

스키마 변경 없음. `POST /api/reviews` 호출 시 서버가 **해당 유저의 해당 강의 진도율 ≥ 80%** 를 확인하고 아니면 422 반환.

### 3-8. 기존 테이블 — 변경 없음

- `refresh_tokens`, `categories`, `sections`, `enrollments`, `lecture_progress`: P1 그대로

---

## 4. P2 ERD (변경 후 최종)

```
┌──────────────────────┐      ┌──────────────────────┐
│ users                │      │ refresh_tokens       │
├──────────────────────┤  1:1 ├──────────────────────┤
│ id (PK)              │◄─────┤ user_id (FK, UNIQUE) │
│ username (UQ,15)     │      │ token                │
│ password             │      │ expiry_date          │
│ role  🆕             │      │ created_at / updated │
│   STUDENT|INSTRUCTOR │      └──────────────────────┘
│ created_at / updated │
└───┬────┬────┬────────┘
    │    │    │
1:1 │    │    │ 1:N  (주문자·장바구니 소유자·북마크 소유자)
    ▼    │    │
┌──────────────────────┐
│ instructor_profiles🆕│
└──────────────────────┘
         │
    1:N  │ (강사로서)
         ▼
      ┌────────────────────────────────────────────┐
      │ courses                                    │
      ├────────────────────────────────────────────┤
      │ id (PK)                                    │
      │ instructor_id (FK→users.id) 🆕             │
      │ category_id (FK)                           │
      │ title / description / difficulty           │
      │ instructor_name  ⚠ deprecated              │
      │ price            🆕 BIGINT                  │
      │ publish_status   🆕 DRAFT|PUBLISHED|ARCHIVED│
      │ published_at     🆕 nullable                │
      │ deleted_at                                 │
      └─┬──────┬─────────┬─────────┬───────────────┘
        │      │         │         │
        │ 1:N  │ 1:N     │ 1:N     │ 1:N
        ▼      ▼         ▼         ▼
   ┌─────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
   │sections │ │enrollments│ │cart_items │ │order_items│
   └────┬────┘ └─────┬─────┘ │    🆕     │ │    🆕     │
        │            │        └───────────┘ └─────┬─────┘
        │ 1:N        │ 1:N                        │ N:1
        ▼            ▼                            ▼
   ┌────────┐  ┌──────────────────┐         ┌─────────┐
   │lectures│  │ lecture_progress │         │ orders  │
   │        │  │                  │         │   🆕    │
   │duration│  │ + playback_pos🆕 │         └──┬──────┘
   │_seconds│  └──────────────────┘            │ 1:N
   │  🆕    │                                  ├──────────┐
   └───┬────┘                                  ▼          ▼
       │                                  ┌─────────┐ ┌────────┐
       │ 1:N                              │payments │ │refunds │
       ▼                                  │   🆕    │ │   🆕   │
  ┌──────────┐                            └─────────┘ └────────┘
  │bookmarks │  🆕
  └──────────┘
```

요약: P1의 엔티티 집합 + 🆕 7종 테이블(`instructor_profiles`, `cart_items`, `orders`, `order_items`, `payments`, `refunds`, `playback_positions`, `bookmarks`).

---

## 5. SQL — 수동 실행용 변경 스크립트

`ddl-auto=update`로 자동 반영되지만, **production-like 환경**에서는 아래 SQL을 먼저 실행한 뒤 엔티티 배포가 더 안전하다. 학습용 MySQL에도 동일 스크립트를 기록해 둔다.

### 5-1. `users` role 추가

```sql
ALTER TABLE users
  ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STUDENT';

-- 기존 사용자 중 강사로 승급할 대상 수동 업데이트(예시)
-- UPDATE users SET role = 'INSTRUCTOR' WHERE username = 'jes0131';
```

### 5-2. `instructor_profiles` 신설

```sql
CREATE TABLE instructor_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  display_name VARCHAR(50) NOT NULL,
  bio TEXT,
  career_years INT,
  profile_image_url VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_instructor_profile_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5-3. `courses` 변경

```sql
ALTER TABLE courses
  ADD COLUMN instructor_id BIGINT NULL AFTER category_id,
  ADD COLUMN publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  ADD COLUMN published_at DATETIME NULL,
  ADD COLUMN price BIGINT NOT NULL DEFAULT 0,
  ADD CONSTRAINT fk_courses_instructor
    FOREIGN KEY (instructor_id) REFERENCES users(id);

-- Backfill: 기존 P1 강의는 기본 강사 계정(예: id=1)에 귀속 + 즉시 PUBLISHED + 무료
UPDATE courses
SET instructor_id = 1,
    publish_status = 'PUBLISHED',
    published_at = NOW(),
    price = 0
WHERE instructor_id IS NULL;

-- Backfill 완료 후 NOT NULL 강화
ALTER TABLE courses
  MODIFY COLUMN instructor_id BIGINT NOT NULL;
```

### 5-4. `lectures` 변경

```sql
ALTER TABLE lectures
  ADD COLUMN duration_seconds INT NOT NULL DEFAULT 0;
```

### 5-5. 장바구니 · 주문 · 결제 · 환불 신설 🆕

```sql
CREATE TABLE cart_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uq_cart_user_course (user_id, course_id),
  CONSTRAINT fk_cart_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
  CONSTRAINT fk_cart_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_no VARCHAR(30) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,
  total_amount BIGINT NOT NULL,
  refunded_amount BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  paid_at DATETIME NULL,
  cancelled_at DATETIME NULL,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  course_title_snapshot VARCHAR(255) NOT NULL,
  price_snapshot BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PAID',
  refunded_at DATETIME NULL,
  CONSTRAINT fk_oi_order  FOREIGN KEY (order_id)  REFERENCES orders(id)  ON DELETE CASCADE,
  CONSTRAINT fk_oi_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  amount BIGINT NOT NULL,
  method VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  mock_transaction_id VARCHAR(50) NOT NULL,
  paid_at DATETIME NOT NULL,
  CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refunds (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  amount BIGINT NOT NULL,
  reason VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  refunded_at DATETIME NOT NULL,
  CONSTRAINT fk_ref_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5-6. 이어듣기 · 북마크 신설 🆕

```sql
CREATE TABLE playback_positions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enrollment_id BIGINT NOT NULL,
  lecture_id BIGINT NOT NULL,
  position_seconds INT NOT NULL DEFAULT 0,
  last_played_at DATETIME NOT NULL,
  UNIQUE KEY uq_playback (enrollment_id, lecture_id),
  CONSTRAINT fk_pp_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
  CONSTRAINT fk_pp_lecture    FOREIGN KEY (lecture_id)    REFERENCES lectures(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bookmarks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lecture_id BIGINT NOT NULL,
  position_seconds INT NOT NULL,
  memo VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_bm_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
  CONSTRAINT fk_bm_lecture FOREIGN KEY (lecture_id) REFERENCES lectures(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5-7. 인덱스

```sql
-- 강사 대시보드
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_publish_status ON courses(publish_status);
CREATE INDEX idx_courses_instructor_publish ON courses(instructor_id, publish_status);

-- 주문 내역 조회
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_order_items_course ON order_items(course_id);

-- 결제/환불 집계
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_refunds_order  ON refunds(order_id);

-- 이어듣기/북마크
CREATE INDEX idx_playback_enrollment_last
  ON playback_positions(enrollment_id, last_played_at DESC);
CREATE INDEX idx_bookmarks_user_lecture
  ON bookmarks(user_id, lecture_id);
```

---

## 6. 마이그레이션 전략

**기존 P1 데이터를 보존한 채로 P2로 전환하는 5-step 절차**:

| 단계 | 작업 | 롤백 |
|------|------|------|
| **1** | `users.role` 추가 (default `'STUDENT'`). 강사로 승급할 계정만 `UPDATE` | `ALTER TABLE users DROP COLUMN role;` |
| **2** | `instructor_profiles` CREATE. 각 강사 계정마다 최소 1개 row 삽입 | `DROP TABLE instructor_profiles;` |
| **3** | `courses`에 `instructor_id`(nullable), `publish_status`, `published_at`, `price` 추가 → **backfill** (기본 강사에 귀속, 전부 PUBLISHED, price=0) → `NOT NULL` 강화 | 컬럼 삭제 |
| **4** 🆕 | `lectures.duration_seconds` 추가 (default 0). 기존 렉처는 강사가 수동 보정 | `DROP COLUMN` |
| **5** 🆕 | 장바구니·주문·결제·환불·이어듣기·북마크 7개 테이블 CREATE. 기존 `enrollments`는 **과거 무료 가입분으로 간주** — 별도 `orders` 레코드를 만들지 않고 그대로 둔다 | 해당 테이블 DROP |

**주의사항**
- `instructor_name` 컬럼은 **제거하지 않는다.** P1 클라이언트와의 하위호환 + 강의 카드에서 즉시 보일 문자열 필요. P3에서 `InstructorProfile.display_name` 참조로 완전 전환.
- 강의 응답 DTO는 P2부터 `instructorId`(Long), `instructorName`(String, 파생) 두 필드를 모두 노출.
- 기존 P1의 무료 `enrollments`에 대응하는 `orders` 레코드는 **생성하지 않는다.** 과거 데이터는 거래 이력 없음으로 두고, P2 이후의 모든 수강 진입은 주문을 반드시 거친다(무료 강의도 0원 주문).
- 주문 상태 머신: `PENDING` → `PAID` → (옵션) `REFUNDED` / `PARTIAL_REFUNDED`. `PENDING`에서 결제 실패 시 `CANCELLED`.

---

## 7. JPA 엔티티 변경 요약 (코드 스켈레톤)

### 7-1. `User.java` — role 추가

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 15)
    private String username;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;   // 🆕

    // createdAt / updatedAt 그대로

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role == null ? Role.STUDENT : role;
    }

    public void promoteToInstructor() { this.role = Role.INSTRUCTOR; }

    public enum Role { STUDENT, INSTRUCTOR }
}
```

### 7-2. `InstructorProfile.java` — 신규 엔티티

```java
@Entity
@Table(name = "instructor_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InstructorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "career_years")
    private Integer careerYears;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 정적 팩토리 + update 메서드
}
```

### 7-3. `Course.java` — instructor_id / publish_status / price

```java
@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instructor_id", nullable = false)  // 🆕
    private Long instructorId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String difficulty;

    @Column(name = "instructor_name")                  // 유지 (deprecate 예정)
    @Deprecated
    private String instructorName;

    @Enumerated(EnumType.STRING)                        // 🆕
    @Column(name = "publish_status", nullable = false, length = 20)
    private PublishStatus publishStatus = PublishStatus.DRAFT;

    @Column(name = "published_at")                      // 🆕
    private LocalDateTime publishedAt;

    @Column(nullable = false)                           // 🆕
    private Long price = 0L;

    private LocalDateTime deletedAt;

    public enum PublishStatus { DRAFT, PUBLISHED, ARCHIVED }

    public void publish() {
        this.publishStatus = PublishStatus.PUBLISHED;
        if (this.publishedAt == null) this.publishedAt = LocalDateTime.now();
    }
    public void archive() { this.publishStatus = PublishStatus.ARCHIVED; }
    public boolean isOwnedBy(Long userId) { return this.instructorId.equals(userId); }
    public boolean isFree() { return this.price != null && this.price == 0L; }
}
```

### 7-4. 주문·결제 엔티티 스켈레톤 🆕

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 30)
    private String orderNo;         // ORD-20260421-0001

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long refundedAmount = 0L;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    public enum Status { PENDING, PAID, CANCELLED, REFUNDED, PARTIAL_REFUNDED }

    public boolean isOwnedBy(Long uid) { return this.userId.equals(uid); }

    public void markPaid() {
        this.status = Status.PAID;
        this.paidAt = LocalDateTime.now();
    }
}
```

### 7-5. 이어듣기·북마크 엔티티 스켈레톤 🆕

```java
@Entity
@Table(
  name = "playback_positions",
  uniqueConstraints = @UniqueConstraint(columnNames = {"enrollment_id", "lecture_id"})
)
public class PlaybackPosition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long enrollmentId;
    private Long lectureId;
    private Integer positionSeconds = 0;
    private LocalDateTime lastPlayedAt;

    public void updatePosition(int sec) {
        this.positionSeconds = sec;
        this.lastPlayedAt = LocalDateTime.now();
    }
}

@Entity
@Table(name = "bookmarks")
public class Bookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long lectureId;
    private Integer positionSeconds;
    @Column(length = 500)
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isOwnedBy(Long uid) { return this.userId.equals(uid); }
}
```

---

## 8. 관계 요약

| From | To | 관계 | FK | 비고 |
|------|-----|------|-----|------|
| `refresh_tokens` | `users` | N:1 (실질 1:1) | `user_id` UNIQUE | P1 그대로 |
| `instructor_profiles` | `users` | 1:1 | `user_id` UNIQUE | 🆕. role=INSTRUCTOR만 생성 |
| `courses` | `users` (강사) | N:1 | `instructor_id` | 🆕 |
| `courses` | `categories` | N:1 | `category_id` | P1 그대로 |
| `sections` | `courses` | N:1 | `course_id` | P1 그대로 |
| `lectures` | `sections` | N:1 | `section_id` | P1 그대로 |
| `enrollments` | `users`, `courses` | N:1, N:1 | `user_id`, `course_id` + UNIQUE | P1 그대로 (결제 성공 시 서비스가 생성) |
| `lecture_progress` | `enrollments`, `lectures` | N:1, N:1 | + UNIQUE | P1 그대로 |
| `reviews` | `users`, `courses` | N:1, N:1 | | 스키마 유지, 80% 진도 게이트는 서비스 레이어 |
| `cart_items` 🆕 | `users`, `courses` | N:1, N:1 | + UNIQUE(user,course) | |
| `orders` 🆕 | `users` | N:1 | `user_id` | |
| `order_items` 🆕 | `orders`, `courses` | N:1, N:1 | | 강의별 스냅샷 |
| `payments` 🆕 | `orders` | N:1 | `order_id` | |
| `refunds` 🆕 | `orders` | N:1 | `order_id` | |
| `playback_positions` 🆕 | `enrollments`, `lectures` | N:1, N:1 | + UNIQUE | upsert |
| `bookmarks` 🆕 | `users`, `lectures` | N:1, N:1 | | UNIQUE 없음 (여러 개 허용) |

---

## 9. 열거형(enum) 정의 모음

| 위치 | 값 | 문자열 저장 |
|------|-----|-----------|
| `User.Role` | `STUDENT`, `INSTRUCTOR` | VARCHAR(20) |
| `Course.PublishStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` | VARCHAR(20) |
| `difficulty` (Course) | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` (기존, 문자열 그대로) | VARCHAR |
| `Order.Status` 🆕 | `PENDING`, `PAID`, `CANCELLED`, `REFUNDED`, `PARTIAL_REFUNDED` | VARCHAR(20) |
| `OrderItem.Status` 🆕 | `PAID`, `REFUNDED` | VARCHAR(20) |
| `Payment.Method` 🆕 | `MOCK_CARD` (P2 고정) | VARCHAR(20) |
| `Payment.Status` 🆕 | `SUCCESS`, `FAILED` | VARCHAR(20) |
| `Refund.Reason` 🆕 | `USER_REQUEST`, `COURSE_CANCELLED`, `CAPACITY_EXCEEDED` | VARCHAR(50) |
| `Refund.Status` 🆕 | `SUCCESS` | VARCHAR(20) |
