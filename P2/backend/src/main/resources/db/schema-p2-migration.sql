-- DevLearn P2 — schema migration
--
-- 적용 순서:
--   1) P1 코드를 P2로 복사하여 본 프로젝트로 기동 (ddl-auto=update)
--      → Hibernate가 users.role, instructor_profiles, courses.instructor_id/publish_status/published_at 를 자동 ALTER/CREATE
--   2) 본 SQL을 수동 실행 (backfill + NOT NULL 강화 + 인덱스)
--   3) seed.sql 실행

-- ----------------------------------------------------------------------
-- 1) users.role 백필
-- ----------------------------------------------------------------------
-- Hibernate가 default=STUDENT로 ALTER 하지만, 이미 NULL로 들어간 row 보정
UPDATE users
SET role = 'STUDENT'
WHERE role IS NULL OR role = '';

-- ----------------------------------------------------------------------
-- 2) courses backfill
-- ----------------------------------------------------------------------
-- 기존 강의에 instructor_id가 비어있다면 기본 강사 계정(id=1)로 귀속
-- seed.sql을 먼저 돌리지 않은 상태라면 유저 id 1이 없을 수 있으니 주의.
UPDATE courses
SET instructor_id = 1,
    publish_status = 'PUBLISHED',
    published_at = NOW()
WHERE instructor_id IS NULL;

-- publish_status가 비어있으면 PUBLISHED로 설정 (P1 시절 강의 공개 유지)
UPDATE courses
SET publish_status = 'PUBLISHED',
    published_at = COALESCE(published_at, NOW())
WHERE publish_status IS NULL OR publish_status = '';

-- ----------------------------------------------------------------------
-- 3) NOT NULL 강화 + FK (backfill 완료 후)
-- ----------------------------------------------------------------------
ALTER TABLE courses
    MODIFY COLUMN instructor_id BIGINT NOT NULL;

-- FK 제약이 아직 없다면 추가 (이미 존재 시 에러 나므로 한 번만 실행)
-- ALTER TABLE courses
--     ADD CONSTRAINT fk_courses_instructor
--     FOREIGN KEY (instructor_id) REFERENCES users(id);

-- ----------------------------------------------------------------------
-- 4) 인덱스
-- ----------------------------------------------------------------------
-- 이미 존재하면 에러가 나므로 한 번만 실행 (또는 IF NOT EXISTS 사용 가능한 MySQL 8+)
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_publish_status ON courses(publish_status);
CREATE INDEX idx_courses_instructor_publish ON courses(instructor_id, publish_status);
