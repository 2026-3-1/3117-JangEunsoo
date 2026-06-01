# 08. 샘플 데이터 — DevLearn P2

`backend/src/main/resources/db/seed.sql` 로 제공. **앱 최초 기동(JPA가 테이블 생성) 후** 수동으로 1회 실행한다.

## 1. 카테고리

```sql
INSERT IGNORE INTO categories (id, name) VALUES
    (1, '백엔드'),
    (2, '프론트엔드'),
    (3, '데이터베이스'),
    (4, 'DevOps'),
    (5, '컴퓨터과학');
```

## 2. 샘플 사용자 (개발용)

비밀번호는 모두 `password1!` (BCrypt 해시 결과로 INSERT).

| id | username | role | 역할 |
|----|----------|------|------|
| 1 | admin | INSTRUCTOR | 기존 P1 데이터를 인수받는 시스템 기본 강사 |
| 2 | tom | INSTRUCTOR | 백엔드 강사 |
| 3 | jane | INSTRUCTOR | 프론트엔드 강사 |
| 4 | alice | STUDENT | 일반 학생 |
| 5 | bob | STUDENT | 일반 학생 |

```sql
-- BCrypt 해시는 환경별로 생성. 예: $2a$10$... (password1!의 해시)
INSERT IGNORE INTO users (id, username, password, role, created_at, updated_at) VALUES
    (1, 'admin', '<bcrypt>', 'INSTRUCTOR', NOW(), NOW()),
    (2, 'tom',   '<bcrypt>', 'INSTRUCTOR', NOW(), NOW()),
    (3, 'jane',  '<bcrypt>', 'INSTRUCTOR', NOW(), NOW()),
    (4, 'alice', '<bcrypt>', 'STUDENT',    NOW(), NOW()),
    (5, 'bob',   '<bcrypt>', 'STUDENT',    NOW(), NOW());
```

## 3. 강사 프로필

```sql
INSERT IGNORE INTO instructor_profiles
    (user_id, display_name, bio, career_years, profile_image_url, created_at, updated_at)
VALUES
    (1, '운영자', '시스템 기본 강사 계정입니다.', 0, NULL, NOW(), NOW()),
    (2, '톰 강사', '10년차 백엔드 개발자, Spring·DB 강의 전문', 10, NULL, NOW(), NOW()),
    (3, '제인 강사', 'React 7년, 디자인 시스템에 관심', 7, NULL, NOW(), NOW());
```

## 4. 샘플 강의

| id | instructor | category | title | price | status |
|----|-----------|----------|-------|-------|--------|
| 1 | tom (2) | 백엔드 (1) | Spring Boot 입문 | 0 (무료) | PUBLISHED |
| 2 | tom (2) | 백엔드 (1) | JPA로 배우는 DB 설계 | 49000 | PUBLISHED |
| 3 | jane (3) | 프론트엔드 (2) | React 19 실전 | 39000 | PUBLISHED |
| 4 | jane (3) | 프론트엔드 (2) | Tailwind 4 마스터 | 0 (무료) | PUBLISHED |
| 5 | tom (2) | DevOps (4) | Docker로 배포하기 | 29000 | DRAFT (학생 비공개) |

```sql
INSERT IGNORE INTO courses (id, instructor_id, category_id, title, description, difficulty,
                            publish_status, published_at, price, created_at, updated_at)
VALUES
    (1, 2, 1, 'Spring Boot 입문', '처음 시작하는 Spring Boot', '초급', 'PUBLISHED', NOW(), 0,     NOW(), NOW()),
    (2, 2, 1, 'JPA로 배우는 DB 설계', '실무 JPA 패턴', '중급',          'PUBLISHED', NOW(), 49000, NOW(), NOW()),
    (3, 3, 2, 'React 19 실전', '훅과 새 기능 정복', '중급',              'PUBLISHED', NOW(), 39000, NOW(), NOW()),
    (4, 3, 2, 'Tailwind 4 마스터', '디자인 시스템까지', '초급',            'PUBLISHED', NOW(), 0,     NOW(), NOW()),
    (5, 2, 4, 'Docker로 배포하기', '아직 작성 중...', '중급',              'DRAFT',     NULL,  29000, NOW(), NOW());
```

## 5. 섹션 & 강의차시 (강의 1번 예시)

```sql
INSERT IGNORE INTO sections (id, course_id, title, order_num) VALUES
    (1, 1, '시작하기', 1),
    (2, 1, '핵심 개념', 2);

INSERT IGNORE INTO lectures (id, section_id, title, video_url, order_num, duration_seconds) VALUES
    (1, 1, '환경 설정',        'https://example.com/v/1', 1, 600),
    (2, 1, '첫 번째 컨트롤러',  'https://example.com/v/2', 2, 720),
    (3, 2, '의존성 주입',      'https://example.com/v/3', 1, 900),
    (4, 2, '데이터 영속화',    'https://example.com/v/4', 2, 1100);
```

> 다른 강의(2~4)에도 비슷한 양의 섹션/강의차시를 시드한다. 양이 많으니 학습용으로 4~6개 정도면 충분.

## 6. 샘플 수강 (선택)

`alice`(id=4)가 무료 강의 1, 4 수강 중:

```sql
INSERT IGNORE INTO enrollments (user_id, course_id, created_at) VALUES
    (4, 1, NOW()),
    (4, 4, NOW());
```

## 7. 비밀번호 평문 (개발 편의용)

| username | password |
|----------|----------|
| admin    | password1! |
| tom      | password1! |
| jane     | password1! |
| alice    | password1! |
| bob      | password1! |

BCrypt 해시 생성은 다음 중 한 방법:

```java
// 1회용 main 클래스
public static void main(String[] args) {
    System.out.println(new BCryptPasswordEncoder().encode("password1!"));
}
```

또는 Spring Shell, jbcrypt 명령행 도구 사용.

## 8. 시드 실행 절차

```bash
# 1) 최초 1회: 빈 스키마 생성
mysql -u root -p -e "CREATE DATABASE devlearn_p2 CHARACTER SET utf8mb4;"

# 2) 앱 기동 (JPA가 테이블 자동 생성)
./gradlew bootRun

# 3) 앱 종료 후 마이그레이션 SQL (P1에서 올라온 경우만)
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/schema-p2-migration.sql

# 4) seed
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/seed.sql
```

## 9. 데이터 초기화

```sql
-- 빠른 초기화 (개발 중)
DROP DATABASE devlearn_p2;
CREATE DATABASE devlearn_p2 CHARACTER SET utf8mb4;
-- 이후 앱 재기동 + seed 다시
```
