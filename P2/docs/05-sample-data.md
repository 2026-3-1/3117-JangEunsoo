# 05. 샘플 데이터 — DevLearn P2

개발·테스트·데모 시 사용할 **신규 seed 데이터**를 정의한다. P1의 `docs/seed-courses.sql`은 참고만 하고, P2용으로 **role·instructor_id·publish_status 컬럼을 포함한 새 세트**를 작성한다.

---

## 1. 구성 개요

| 리소스 | 개수 | 비고 |
|--------|-----|-----|
| 카테고리 | 5 | 백엔드 / 프론트 / DB / DevOps / CS |
| 강사 계정 | 4 | `instructor1` ~ `instructor4` |
| 학생 계정 | 3 | `student1` ~ `student3` |
| 강사 프로필 | 4 | 강사 계정과 1:1 |
| 강의 | 9 | PUBLISHED 7 + DRAFT 1 + ARCHIVED 1 |
| 섹션 | 18 | 강의당 1~3 |
| 렉처 | 42 | 섹션당 2~3 |

모든 비밀번호는 **BCrypt로 해싱된 `Password123!`** 을 사용한다.

```
BCrypt hash of "Password123!":
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

> 실제 seed 적용 시에는 `BCryptPasswordEncoder().encode("Password123!")`의 최신 해시를 사용할 것. 위 해시는 예시.

---

## 2. 테스트 계정

| username | 역할 | 비밀번호 | 메모 |
|----------|-----|---------|-----|
| `instructor1` | INSTRUCTOR | Password123! | 메인 강사 (3강의) |
| `instructor2` | INSTRUCTOR | Password123! | 프론트 전문 (2강의) |
| `instructor3` | INSTRUCTOR | Password123! | DB/인프라 (2강의) |
| `instructor4` | INSTRUCTOR | Password123! | 신규 강사 (DRAFT만 보유) |
| `student1` | STUDENT | Password123! | 일반 수강생 |
| `student2` | STUDENT | Password123! | 일반 수강생 |
| `student3` | STUDENT | Password123! | 일반 수강생 |

> P1 스키마에 email 컬럼이 없으므로 test@domain 형식이 아닌 **username**만 사용.

---

## 3. 카테고리 Seed

```sql
INSERT INTO categories (id, name, slug, created_at, updated_at) VALUES
  (1, '백엔드',   'backend',   NOW(), NOW()),
  (2, '프론트엔드','frontend',  NOW(), NOW()),
  (3, '데이터베이스','database', NOW(), NOW()),
  (4, 'DevOps',   'devops',    NOW(), NOW()),
  (5, '컴퓨터과학','cs',       NOW(), NOW());
```

> 카테고리 컬럼은 P1 실제 스키마와 맞출 것. `slug` 컬럼이 없다면 생략.

---

## 4. 사용자 Seed

```sql
-- ========== INSTRUCTORS ==========
INSERT INTO users (id, username, password, role, created_at, updated_at) VALUES
  (1, 'instructor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INSTRUCTOR', NOW(), NOW()),
  (2, 'instructor2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INSTRUCTOR', NOW(), NOW()),
  (3, 'instructor3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INSTRUCTOR', NOW(), NOW()),
  (4, 'instructor4', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INSTRUCTOR', NOW(), NOW());

-- ========== STUDENTS ==========
INSERT INTO users (id, username, password, role, created_at, updated_at) VALUES
  (5, 'student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', NOW(), NOW()),
  (6, 'student2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', NOW(), NOW()),
  (7, 'student3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', NOW(), NOW());
```

---

## 5. 강사 프로필 Seed

```sql
INSERT INTO instructor_profiles (
  user_id, display_name, bio, career_years, profile_image_url, created_at, updated_at
) VALUES
  (1, '김백엔드',
      '10년차 백엔드 엔지니어. 대규모 트래픽 시스템 설계와 스프링 생태계 전문.',
      10, NULL, NOW(), NOW()),
  (2, '이프론트',
      '프론트엔드 개발자 7년. React, TypeScript, 디자인 시스템 경험.',
      7, NULL, NOW(), NOW()),
  (3, '박데이터',
      'DB 튜닝과 인프라 자동화를 가르치는 12년차 시니어.',
      12, NULL, NOW(), NOW()),
  (4, '신입강사',
      '첫 강의를 준비 중입니다. 잘 부탁드려요!',
      1, NULL, NOW(), NOW());
```

---

## 6. 강의 Seed

```sql
-- ========== instructor1: 3 courses (2 PUBLISHED + 1 ARCHIVED) ==========
INSERT INTO courses (
  id, title, description, difficulty, category_id,
  instructor_id, instructor_name, publish_status, published_at,
  created_at, updated_at
) VALUES
  (1, '스프링부트 입문',
      '자바 백엔드의 사실상 표준. 프로젝트 생성부터 JPA, Security까지 훑어봅니다.',
      'BEGINNER', 1,
      1, '김백엔드', 'PUBLISHED', '2026-03-01 10:00:00',
      '2026-02-20 09:00:00', '2026-03-01 10:00:00'),

  (2, 'JPA 실전',
      '엔티티 설계, 연관관계, 성능 튜닝까지 현업 JPA 사용법.',
      'INTERMEDIATE', 1,
      1, '김백엔드', 'PUBLISHED', '2026-03-15 10:00:00',
      '2026-03-01 09:00:00', '2026-03-15 10:00:00'),

  (3, '레거시 스프링 3 강의',
      '구버전 커리큘럼. 더 이상 업데이트하지 않습니다.',
      'BEGINNER', 1,
      1, '김백엔드', 'ARCHIVED', '2025-11-01 10:00:00',
      '2025-10-01 09:00:00', '2026-01-15 12:00:00');

-- ========== instructor2: 2 courses (2 PUBLISHED) ==========
INSERT INTO courses (
  id, title, description, difficulty, category_id,
  instructor_id, instructor_name, publish_status, published_at,
  created_at, updated_at
) VALUES
  (4, 'React 18 완전 정복',
      '훅, Suspense, 동시성 렌더링까지 모던 React의 모든 것.',
      'INTERMEDIATE', 2,
      2, '이프론트', 'PUBLISHED', '2026-03-10 10:00:00',
      '2026-02-25 09:00:00', '2026-03-10 10:00:00'),

  (5, 'TypeScript 타입 체조',
      '제네릭, 조건부 타입, 유틸리티 타입으로 타입 안전성 끌어올리기.',
      'ADVANCED', 2,
      2, '이프론트', 'PUBLISHED', '2026-03-20 10:00:00',
      '2026-03-05 09:00:00', '2026-03-20 10:00:00');

-- ========== instructor3: 2 courses (2 PUBLISHED) ==========
INSERT INTO courses (
  id, title, description, difficulty, category_id,
  instructor_id, instructor_name, publish_status, published_at,
  created_at, updated_at
) VALUES
  (6, 'MySQL 성능 튜닝',
      '인덱스 설계, 실행계획 분석, 잠금 이슈 해결까지.',
      'ADVANCED', 3,
      3, '박데이터', 'PUBLISHED', '2026-03-12 10:00:00',
      '2026-02-28 09:00:00', '2026-03-12 10:00:00'),

  (7, 'Docker & Compose 기초',
      '컨테이너 개념부터 개발환경 구성, 배포까지 핸즈온.',
      'BEGINNER', 4,
      3, '박데이터', 'PUBLISHED', '2026-03-18 10:00:00',
      '2026-03-03 09:00:00', '2026-03-18 10:00:00');

-- ========== instructor4: 1 course (DRAFT only - 학생에겐 안 보임) ==========
INSERT INTO courses (
  id, title, description, difficulty, category_id,
  instructor_id, instructor_name, publish_status, published_at,
  created_at, updated_at
) VALUES
  (8, '자료구조 입문 (준비중)',
      '배열, 연결리스트, 트리, 해시테이블을 파이썬으로.',
      'BEGINNER', 5,
      4, '신입강사', 'DRAFT', NULL,
      '2026-04-10 09:00:00', '2026-04-15 12:00:00');

-- ========== 추가: instructor2의 DRAFT 1개 (강사 콘솔 테스트용) ==========
INSERT INTO courses (
  id, title, description, difficulty, category_id,
  instructor_id, instructor_name, publish_status, published_at,
  created_at, updated_at
) VALUES
  (9, 'Next.js 14 (작성중)',
      'App Router와 서버 컴포넌트로 짜는 현업 Next.js.',
      'INTERMEDIATE', 2,
      2, '이프론트', 'DRAFT', NULL,
      '2026-04-05 09:00:00', '2026-04-15 09:00:00');
```

---

## 7. 섹션 & 렉처 Seed (예시)

강의 #1 "스프링부트 입문" 한 건의 전체 구조를 보이고, 나머지는 패턴을 공유한다.

```sql
-- Course 1: 스프링부트 입문
INSERT INTO sections (id, course_id, title, sort_order, created_at, updated_at) VALUES
  (1, 1, '1. 시작하기',      0, NOW(), NOW()),
  (2, 1, '2. 웹 계층',        1, NOW(), NOW()),
  (3, 1, '3. 데이터 접근',    2, NOW(), NOW());

INSERT INTO lectures (id, section_id, title, duration_seconds, sort_order, created_at, updated_at) VALUES
  -- 섹션 1
  (1, 1, '강의 소개',            240, 0, NOW(), NOW()),
  (2, 1, '스프링부트 프로젝트 생성', 600, 1, NOW(), NOW()),
  (3, 1, 'Hello World 컨트롤러', 420, 2, NOW(), NOW()),
  -- 섹션 2
  (4, 2, 'REST 컨트롤러',        720, 0, NOW(), NOW()),
  (5, 2, 'Validation',          540, 1, NOW(), NOW()),
  -- 섹션 3
  (6, 3, 'JPA 엔티티 기초',     780, 0, NOW(), NOW()),
  (7, 3, '레포지토리와 쿼리',    660, 1, NOW(), NOW()),
  (8, 3, '트랜잭션',            480, 2, NOW(), NOW());
```

### 나머지 강의의 렉처 구성 요약

| 강의 ID | 제목 | 섹션 수 | 렉처 수 |
|--------|------|--------|--------|
| 1 | 스프링부트 입문 | 3 | 8 |
| 2 | JPA 실전 | 3 | 7 |
| 3 | 레거시 스프링 3 강의 (ARCHIVED) | 2 | 4 |
| 4 | React 18 완전 정복 | 3 | 7 |
| 5 | TypeScript 타입 체조 | 2 | 5 |
| 6 | MySQL 성능 튜닝 | 2 | 5 |
| 7 | Docker & Compose 기초 | 2 | 4 |
| 8 | 자료구조 입문 (DRAFT) | 1 | 2 |
| 9 | Next.js 14 (DRAFT) | 0 | 0 |
| **합계** | | **18** | **42** |

---

## 8. 수강/진도/리뷰 Seed (데모용)

```sql
-- student1이 강의 1, 4를 수강
INSERT INTO enrollments (id, user_id, course_id, created_at) VALUES
  (1, 5, 1, '2026-03-20 10:00:00'),
  (2, 5, 4, '2026-03-22 11:00:00');

-- student1이 강의 1의 렉처 1~3 완료 (진도 37.5%)
INSERT INTO lecture_progress (id, enrollment_id, lecture_id, completed_at, created_at, updated_at) VALUES
  (1, 1, 1, '2026-03-20 10:05:00', NOW(), NOW()),
  (2, 1, 2, '2026-03-20 10:20:00', NOW(), NOW()),
  (3, 1, 3, '2026-03-21 09:10:00', NOW(), NOW());

-- student2가 강의 1 수강, 50% 진도 + 리뷰 작성
INSERT INTO enrollments (id, user_id, course_id, created_at) VALUES
  (3, 6, 1, '2026-03-18 14:00:00');

INSERT INTO lecture_progress (id, enrollment_id, lecture_id, completed_at, created_at, updated_at) VALUES
  (4, 3, 1, '2026-03-18 14:05:00', NOW(), NOW()),
  (5, 3, 2, '2026-03-18 14:25:00', NOW(), NOW()),
  (6, 3, 3, '2026-03-19 09:00:00', NOW(), NOW()),
  (7, 3, 4, '2026-03-19 09:15:00', NOW(), NOW());

INSERT INTO reviews (id, course_id, user_id, rating, content, created_at, updated_at) VALUES
  (1, 1, 6, 5, '설명이 차분하고 예제가 풍부해요!', NOW(), NOW()),
  (2, 4, 5, 4, 'React 훅 설명이 특히 좋았습니다.', NOW(), NOW());
```

---

## 9. 데모 시나리오

### 시나리오 1: 학생 플로우

1. `student1` / `Password123!` 로그인
2. `/courses` → 7개 PUBLISHED 강의 노출 (DRAFT·ARCHIVED 숨김)
3. "스프링부트 입문" 클릭 → 강사 카드(김백엔드, 10년차) 표시
4. 강사 이름 클릭 → `/instructors/1` 강사 프로필
5. "JPA 실전" 수강 → `/my/courses`에서 확인

### 시나리오 2: 강사 플로우

1. `instructor2` / `Password123!` 로그인
2. 네비게이션 바에 "강사 콘솔" 노출 확인
3. `/instructor/courses` → `React 18 완전 정복` (PUBLISHED), `TypeScript 타입 체조` (PUBLISHED), `Next.js 14` (DRAFT) 3건 노출
4. DRAFT 강의 편집 → 섹션/렉처 추가 → 발행 버튼
5. `/instructor/courses/4/students` → 수강생 student1 확인

### 시나리오 3: 권한 우회 시도 (보안 테스트)

1. `student1` 로그인 후 토큰 확보
2. `curl -H "Authorization: Bearer <student_jwt>" -X POST /api/instructor/courses ...`
   → **403**
3. `instructor2` 로그인 후 `PUT /api/instructor/courses/1` (instructor1의 강의) 시도
   → **404** (info-leak 방지)

---

## 10. Seed 적용 방법

### 10-1. 개발 환경

```bash
# MySQL에 접속
mysql -u root -p devlearn_p2

# 스키마가 JPA ddl-auto=update로 생성된 뒤 실행
source P2/backend/src/main/resources/db/seed.sql
```

### 10-2. 스크립트 위치 권장

```
P2/backend/src/main/resources/db/
├── schema-p2-migration.sql   (02-data-model.md의 ALTER/CREATE)
└── seed.sql                   (본 문서의 INSERT 모음)
```

> `CommandLineRunner`로 기동 시 자동 주입하는 방식도 가능. 단, 운영 환경에서는 반드시 비활성화.

---

## 11. 관련 문서

- 데이터 모델/SQL 스키마: [02-data-model.md](./02-data-model.md)
- 구현 순서: [06-implementation-checklist.md](./06-implementation-checklist.md)
