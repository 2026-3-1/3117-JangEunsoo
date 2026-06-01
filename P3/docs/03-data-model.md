# 03. 데이터 모델 — DevLearn P2

## 1. DBMS & 전략

| 항목 | 값 |
|------|-----|
| DBMS | MySQL 8.0+ |
| 문자셋 | `utf8mb4` |
| ORM | Hibernate (Spring Data JPA) |
| DDL 전략 | `spring.jpa.hibernate.ddl-auto=update` |
| 마이그레이션 도구 | **없음** (수동 SQL `schema-p2-migration.sql`) |
| 시간 컬럼 | `created_at` / `updated_at` (Spring Data JPA `@CreatedDate` / `@LastModifiedDate`) |
| 소프트 삭제 | `courses.deleted_at` 만 적용 (`@SQLDelete`/`@SQLRestriction`) |

## 2. ERD (전체)

```
users (id, username UQ(15), password, role[STUDENT|INSTRUCTOR], created_at, updated_at)
  │ 1:1
  ├── refresh_tokens (user_id UQ, token, expiry_date)
  │ 1:1 (role=INSTRUCTOR만)
  ├── instructor_profiles (id, user_id UQ, display_name(50), bio TEXT,
  │                        career_years, profile_image_url(500))
  │ 1:N
  ├── cart_items (id, user_id, course_id, created_at)   UNIQUE(user_id, course_id)
  │ 1:N
  ├── orders (id, order_no UQ(32), user_id, status, total_amount,
  │           refunded_amount, paid_at, created_at, updated_at)
  │             │ 1:N
  │             ├── order_items (id, order_id, course_id,
  │             │                course_title_snapshot(255), price_snapshot,
  │             │                status[ACTIVE|REFUNDED])
  │             │ 1:1
  │             ├── payments (id, order_id, method[MOCK_CARD],
  │             │             status[SUCCESS|FAILED], amount,
  │             │             mock_transaction_id(64), created_at)
  │             │ 1:N
  │             └── refunds (id, order_id, order_item_id, amount,
  │                          reason[USER_REQUEST|COURSE_CANCELLED|...], created_at)
  │ 1:N
  ├── enrollments (id, user_id, course_id, created_at)   UNIQUE(user_id, course_id)
  │     │ 1:N
  │     ├── lecture_progress (id, enrollment_id, lecture_id, is_completed)
  │     │                                                UNIQUE(enrollment_id, lecture_id)
  │     │ 1:N
  │     └── playback_positions (id, enrollment_id, lecture_id, current_time_seconds,
  │                             created_at, updated_at)  UNIQUE(enrollment_id, lecture_id)
  │ 1:N
  ├── reviews (id, user_id, course_id, rating INT, comment TEXT, created_at)
  │ 1:N
  └── bookmarks (id, user_id, lecture_id, time_seconds, memo TEXT, created_at)

courses (id, instructor_id FK→users.id, category_id FK→categories.id,
         title, description TEXT, difficulty, instructor_name (deprecated),
         publish_status[DRAFT|PUBLISHED|ARCHIVED], published_at,
         price BIGINT default 0, deleted_at)
  │ 1:N
  └── sections (id, course_id, title, order_num)
        │ 1:N
        └── lectures (id, section_id, title, video_url, order_num, duration_seconds)

categories (id, name)
```

## 3. 테이블 상세

### 3-1. `users`

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(15) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt 해시) |
| role | VARCHAR(20) | NOT NULL, default `'STUDENT'`, enum `STUDENT`/`INSTRUCTOR` |
| created_at, updated_at | DATETIME | NOT NULL |

### 3-2. `refresh_tokens` (P1과 동일)

P1에서 유지. 변경 없음.

### 3-3. `instructor_profiles` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| user_id | BIGINT | FK→users.id, **UNIQUE**, NOT NULL |
| display_name | VARCHAR(50) | NOT NULL |
| bio | TEXT | nullable |
| career_years | INT | nullable |
| profile_image_url | VARCHAR(500) | nullable (외부 URL) |
| created_at, updated_at | DATETIME | NOT NULL |

> role=INSTRUCTOR 유저만 1:1로 보유. STUDENT는 row 없음.

### 3-4. `categories` (P1과 동일)

`(id BIGINT PK, name VARCHAR NOT NULL)`. seed.sql로 5개 시드(`백엔드`, `프론트엔드`, `데이터베이스`, `DevOps`, `컴퓨터과학`).

### 3-5. `courses` 🔁 (확장)

| 컬럼 | 타입 | 제약 | 비고 |
|------|------|------|------|
| id | BIGINT | PK | |
| instructor_id | BIGINT | NOT NULL, FK→users.id | P2 신규. P1 데이터 마이그레이션 시 1번 사용자에 귀속 |
| category_id | BIGINT | FK→categories.id | |
| title | VARCHAR | NOT NULL | |
| description | TEXT | | |
| difficulty | VARCHAR | | `초급`/`중급`/`고급` 등 자유 텍스트 |
| instructor_name | VARCHAR | nullable, **@Deprecated** | P1 하위호환 필드. 새 코드는 `instructor_id`만 사용 |
| publish_status | VARCHAR(20) | NOT NULL, default `'DRAFT'` | enum: `DRAFT`/`PUBLISHED`/`ARCHIVED` |
| published_at | DATETIME | nullable | PUBLISHED 전이 시 기록 |
| price | BIGINT | NOT NULL, default `0` | 원 단위. 0 = 무료 |
| deleted_at | DATETIME | nullable | soft delete |

**인덱스:**
```sql
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_publish_status ON courses(publish_status);
CREATE INDEX idx_courses_instructor_publish ON courses(instructor_id, publish_status);
```

### 3-6. `sections`, `lectures` (P1과 동일 + duration)

`lectures.duration_seconds INT` 필드는 진도율 계산용. P1에서 이미 있다면 변경 없음.

### 3-7. `enrollments` (P1과 동일)

`UNIQUE(user_id, course_id)`. 환불 시 hard delete.

### 3-8. `lecture_progress` (P1과 동일)

`UNIQUE(enrollment_id, lecture_id)`. 진도율 = (완료된 lecture 수) / (course의 전체 lecture 수) × 100. **서버에서 항상 재계산**.

### 3-9. `reviews` (P1 + 80% 게이트)

스키마 변경 없음. 작성 시 진도율 ≥ 80% 강제 (서비스 로직).

### 3-10. `cart_items` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| user_id | BIGINT | NOT NULL |
| course_id | BIGINT | NOT NULL |
| created_at | DATETIME | NOT NULL |

**제약:** `UNIQUE(user_id, course_id)` — 중복 담기 시 `409 CART_DUPLICATE`

### 3-11. `orders` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| order_no | VARCHAR(32) | UNIQUE, NOT NULL — `YYYYMMDD-seq` 형식 |
| user_id | BIGINT | NOT NULL |
| status | VARCHAR(20) | NOT NULL, default `'PENDING'` |
| total_amount | BIGINT | NOT NULL, default 0 |
| refunded_amount | BIGINT | NOT NULL, default 0 |
| paid_at | DATETIME | nullable |
| created_at, updated_at | DATETIME | NOT NULL |

**status 전이:** `PENDING → PAID → (REFUNDED | PARTIAL_REFUNDED)` 또는 `PENDING → CANCELLED`

### 3-12. `order_items` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| order_id | BIGINT | FK→orders.id, NOT NULL |
| course_id | BIGINT | NOT NULL |
| course_title_snapshot | VARCHAR(255) | NOT NULL, 주문 시점 강의 제목 고정 |
| price_snapshot | BIGINT | NOT NULL, 주문 시점 가격 고정 |
| status | VARCHAR(20) | NOT NULL, default `'ACTIVE'`, enum: `ACTIVE`/`REFUNDED` |

> **스냅샷 보존 원칙**: 강의 가격·제목이 변해도 주문 내역은 변하지 않는다.

### 3-13. `payments` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| order_id | BIGINT | NOT NULL |
| method | VARCHAR(20) | NOT NULL, enum: `MOCK_CARD` |
| status | VARCHAR(20) | NOT NULL, enum: `SUCCESS`/`FAILED` |
| amount | BIGINT | NOT NULL |
| mock_transaction_id | VARCHAR(64) | nullable (UUID 등) |
| created_at | DATETIME | NOT NULL |

### 3-14. `refunds` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| order_id | BIGINT | NOT NULL |
| order_item_id | BIGINT | nullable (전체 환불 시 비워둠) |
| amount | BIGINT | NOT NULL |
| reason | VARCHAR(30) | NOT NULL, enum: `USER_REQUEST`/`COURSE_CANCELLED`/`DUPLICATE_PAYMENT`/`OTHER` |
| created_at | DATETIME | NOT NULL |

### 3-15. `playback_positions` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| enrollment_id | BIGINT | NOT NULL |
| lecture_id | BIGINT | NOT NULL |
| current_time_seconds | INT | NOT NULL, ≥0 |
| created_at, updated_at | DATETIME | NOT NULL |

**제약:** `UNIQUE(enrollment_id, lecture_id)` — 업서트로 운영

### 3-16. `bookmarks` 🆕

| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| user_id | BIGINT | NOT NULL |
| lecture_id | BIGINT | NOT NULL |
| time_seconds | INT | NOT NULL, ≥0 |
| memo | TEXT | nullable |
| created_at | DATETIME | NOT NULL |

> 강의(course)가 아닌 **lecture 단위**. 동일 lecture·동일 time에 여러 개 허용 (UNIQUE 제약 없음).

## 4. Enum 카탈로그

| Enum | 값 |
|------|------|
| `Role` | `STUDENT`, `INSTRUCTOR` |
| `PublishStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `OrderStatus` | `PENDING`, `PAID`, `CANCELLED`, `REFUNDED`, `PARTIAL_REFUNDED` |
| `OrderItemStatus` | `ACTIVE`, `REFUNDED` |
| `PaymentMethod` | `MOCK_CARD` |
| `PaymentStatus` | `SUCCESS`, `FAILED` |
| `RefundReason` | `USER_REQUEST`, `COURSE_CANCELLED`, `DUPLICATE_PAYMENT`, `OTHER` |

모두 `@Enumerated(EnumType.STRING)` 으로 저장.

## 5. 핵심 무결성 규칙

1. `courses.instructor_id NOT NULL` — P1 데이터는 마이그레이션 시 1번 사용자에 귀속
2. `courses.price >= 0` — 음수 금지 (서비스 검증)
3. `order_items.price_snapshot` ≥ 0
4. `orders.total_amount = SUM(order_items.price_snapshot WHERE status='ACTIVE')` — 환불 시 갱신
5. `orders.refunded_amount = SUM(refunds.amount)` — 환불 누적 합
6. 환불 시 enrollment **hard delete** (LectureProgress·PlaybackPosition도 cascade)
7. `Enrollment` 생성 경로는 두 가지:
   - 무료 강의: `POST /api/enrollments` (직접)
   - 유료 강의: 결제 SUCCESS 후 OrderService가 자동 생성
8. `PUBLISHED` 강의만 `/api/courses` 목록·상세에 노출 (소프트 삭제 + DRAFT/ARCHIVED 제외)

## 6. 수동 마이그레이션 SQL (`schema-p2-migration.sql`)

P1 운영 DB에서 P2로 올라갈 때 1회 실행:

```sql
-- 1. users.role 백필
UPDATE users SET role = 'STUDENT' WHERE role IS NULL OR role = '';

-- 2. courses 백필: 고아 강의는 instructor_id=1, PUBLISHED 상태로
UPDATE courses
SET instructor_id = 1,
    publish_status = 'PUBLISHED',
    published_at = NOW()
WHERE instructor_id IS NULL;

-- 3. courses.instructor_id NOT NULL 강제
ALTER TABLE courses MODIFY COLUMN instructor_id BIGINT NOT NULL;

-- 4. 인덱스
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_publish_status ON courses(publish_status);
CREATE INDEX idx_courses_instructor_publish ON courses(instructor_id, publish_status);
```

신규 테이블(`instructor_profiles`, `cart_items`, `orders`, `order_items`, `payments`, `refunds`, `playback_positions`, `bookmarks`)은 **JPA `ddl-auto=update`가 자동 생성**한다. 별도 DDL 불필요.
