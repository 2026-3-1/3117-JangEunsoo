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

### 3-3. `courses` 테이블 — `instructor_id` FK + `publish_status` 추가

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `instructor_id` | `BIGINT` | FK→`users.id`, NOT NULL (신규 강의 기준) — 하위호환 위해 일시적으로 NULL 허용 후 backfill 완료 시 NOT NULL |
| `publish_status` | `VARCHAR(20)` | `'DRAFT'` default. enum: `DRAFT`/`PUBLISHED`/`ARCHIVED` |
| `published_at` | `DATETIME` | nullable. 처음 발행된 시각 (analytics용) |

**`instructor_name`은 한동안 유지.** P3로 넘어가기 전에 제거 후보. JPA 엔티티에선 `@Deprecated` 표시.

### 3-4. 기존 테이블 — 변경 없음

- `refresh_tokens`, `categories`, `sections`, `lectures`, `enrollments`, `lecture_progress`, `reviews`: P1 그대로

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
└─────┬──────┬─────────┘
      │      │
 1:1  │      │ 1:N (as instructor)
      ▼      │
┌──────────────────────┐      ┌──────────────────────┐
│ instructor_profiles🆕│      │ enrollments          │
├──────────────────────┤      ├──────────────────────┤
│ id (PK)              │      │ id (PK)              │
│ user_id (UQ, FK)     │      │ user_id              │
│ display_name         │      │ course_id            │
│ bio (TEXT)           │      │ created_at           │
│ career_years         │      │ UNIQUE(user,course)  │
│ profile_image_url    │      └─────────┬────────────┘
│ created_at / updated │                │
└──────────────────────┘                │ 1:N
                                        ▼
                               ┌──────────────────────┐
                               │ lecture_progress     │
                               ├──────────────────────┤
                               │ id / enrollment_id   │
                               │ lecture_id           │
                               │ is_completed         │
                               └──────────────────────┘

      ┌────────────────────────────────────────────┐
      │ courses                                    │
      ├────────────────────────────────────────────┤
      │ id (PK)                                    │
      │ instructor_id (FK→users.id) 🆕             │
      │ category_id (FK)                           │
      │ title                                      │
      │ description (TEXT)                         │
      │ difficulty                                 │
      │ instructor_name  ⚠ deprecated, 유지        │
      │ publish_status  🆕 DRAFT|PUBLISHED|ARCHIVED│
      │ published_at    🆕 nullable                │
      │ deleted_at                                 │
      └─────┬──────────────────────────────────────┘
            │ 1:N
            ▼
      ┌──────────────┐       ┌──────────────┐
      │ sections     │──1:N─►│ lectures     │
      └──────────────┘       └──────────────┘

      ┌──────────────┐
      │ reviews      │ (P1 그대로)
      └──────────────┘

      ┌──────────────┐
      │ categories   │ (P1 그대로)
      └──────────────┘
```

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
  ADD CONSTRAINT fk_courses_instructor
    FOREIGN KEY (instructor_id) REFERENCES users(id);

-- Backfill: 기존 P1 강의는 기본 강사 계정(예: id=1)에 귀속 + 즉시 PUBLISHED
UPDATE courses
SET instructor_id = 1,
    publish_status = 'PUBLISHED',
    published_at = NOW()
WHERE instructor_id IS NULL;

-- Backfill 완료 후 NOT NULL 강화
ALTER TABLE courses
  MODIFY COLUMN instructor_id BIGINT NOT NULL;
```

### 5-4. 인덱스

```sql
-- 강사 대시보드: "내 강의 목록" 쿼리 가속
CREATE INDEX idx_courses_instructor ON courses(instructor_id);

-- 학생용 목록: 발행된 강의만 필터
CREATE INDEX idx_courses_publish_status ON courses(publish_status);

-- 복합: "특정 강사의 발행 강의" — 강사 프로필 페이지용
CREATE INDEX idx_courses_instructor_publish
  ON courses(instructor_id, publish_status);
```

---

## 6. 마이그레이션 전략

**기존 P1 데이터를 보존한 채로 P2로 전환하는 3-step 절차**:

| 단계 | 작업 | 롤백 |
|------|------|------|
| **1** | `users.role` 추가 (default `'STUDENT'`). 강사로 승급할 계정만 `UPDATE` | `ALTER TABLE users DROP COLUMN role;` |
| **2** | `instructor_profiles` CREATE. 각 강사 계정마다 최소 1개 row 삽입 | `DROP TABLE instructor_profiles;` |
| **3** | `courses`에 `instructor_id`(nullable), `publish_status`, `published_at` 추가 → **backfill** (기본 강사에 귀속, 전부 PUBLISHED) → `NOT NULL` 강화 | 3단계는 컬럼 삭제로 롤백 (`DROP COLUMN`) |

**주의사항**
- `instructor_name` 컬럼은 **제거하지 않는다.** P1 클라이언트와의 하위호환 + 강의 카드에서 즉시 보일 문자열 필요. P3에서 `InstructorProfile.display_name` 참조로 완전 전환.
- 강의 응답 DTO는 P2부터 `instructorId`(Long), `instructorName`(String, 파생) 두 필드를 모두 노출.

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

### 7-3. `Course.java` — instructor_id / publish_status

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

    private LocalDateTime deletedAt;

    public enum PublishStatus { DRAFT, PUBLISHED, ARCHIVED }

    public void publish() {
        this.publishStatus = PublishStatus.PUBLISHED;
        if (this.publishedAt == null) this.publishedAt = LocalDateTime.now();
    }
    public void archive() { this.publishStatus = PublishStatus.ARCHIVED; }
    public boolean isOwnedBy(Long userId) { return this.instructorId.equals(userId); }
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
| `enrollments` | `users`, `courses` | N:1, N:1 | `user_id`, `course_id` + UNIQUE | P1 그대로 |
| `lecture_progress` | `enrollments`, `lectures` | N:1, N:1 | + UNIQUE | P1 그대로 |
| `reviews` | `users`, `courses` | N:1, N:1 | | P1 그대로 |

---

## 9. 열거형(enum) 정의 모음

| 위치 | 값 | 문자열 저장 |
|------|-----|-----------|
| `User.Role` | `STUDENT`, `INSTRUCTOR` | VARCHAR(20) |
| `Course.PublishStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` | VARCHAR(20) |
| `difficulty` (Course) | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` (기존, 문자열 그대로) | VARCHAR |
