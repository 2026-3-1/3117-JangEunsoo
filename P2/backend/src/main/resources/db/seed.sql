-- DevLearn P2 — 최소 seed
--
-- 05-sample-data.md의 full seed는 Phase 3+ 시연용.
-- Phase 1 DoD("instructor1 계정으로 로그인 시 JWT 발급") 확인을 위한 최소 데이터만 둔다.
--
-- 전제:
--   * schema-p2-migration.sql 실행 완료 (backfill 포함)
--   * users 테이블에 최소 1명 이상의 기존 회원이 존재
--
-- 사용법:
--   먼저 현재 회원을 확인한 뒤, 그중 한 명을 INSTRUCTOR로 승급시킨다.

-- 예시: id=1 사용자를 강사로 승급
-- UPDATE users SET role = 'INSTRUCTOR' WHERE id = 1;

-- 강사 프로필 최소 1건 (user_id는 실제 승급한 사용자 id에 맞춰 수정)
-- INSERT INTO instructor_profiles (user_id, display_name, bio, career_years, profile_image_url, created_at, updated_at)
-- VALUES (1, '김백엔드', '10년차 백엔드 엔지니어.', 10, NULL, NOW(), NOW());

-- 카테고리 (아직 없다면)
INSERT IGNORE INTO categories (id, name) VALUES
    (1, '백엔드'),
    (2, '프론트엔드'),
    (3, '데이터베이스'),
    (4, 'DevOps'),
    (5, '컴퓨터과학');
