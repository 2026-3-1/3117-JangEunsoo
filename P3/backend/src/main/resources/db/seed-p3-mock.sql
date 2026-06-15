-- ============================================================
-- DevLearn P3 — Mock Data (시연/개발용)
-- ============================================================
-- 전제:
--   * 테이블은 앱 1회 기동(ddl-auto=update)으로 이미 생성돼 있음
--   * users 에 회원이 이미 존재함 (이 스크립트는 계정을 만들지 않음)
--     - user id 1 = 학생(STUDENT)
--     - user id 2 = 강사(INSTRUCTOR)
--
-- 사용법:
--   mysql -u root -p devlearn < seed-p3-mock.sql
--
-- 멱등성: 카테고리는 INSERT IGNORE. 강의 이하는 매 실행마다 새로 INSERT되므로
--         재실행 시 중복이 싫으면 하단 "초기화" 블록을 먼저 실행할 것.
-- ============================================================

-- ── 대상 사용자 id ───────────────────────────────────────────
SET @instructor_id = 2;   -- 강사
SET @student_id    = 1;   -- 학생(수강생)

-- 강사 프로필이 없으면 생성 (이미 있으면 IGNORE)
INSERT IGNORE INTO instructor_profiles (user_id, display_name, bio, career_years, profile_image_url, created_at, updated_at)
VALUES (@instructor_id, '김자바', '10년차 백엔드 엔지니어. Spring·MySQL 전문.', 10, NULL, NOW(), NOW());

-- ── 카테고리 ──────────────────────────────────────────────
INSERT IGNORE INTO categories (id, name) VALUES
    (1, '백엔드'),
    (2, '프론트엔드'),
    (3, '데이터베이스'),
    (4, 'DevOps'),
    (5, '컴퓨터과학');

-- ── 강의 4개 (무료1 / 유료2 / DRAFT1) ───────────────────────
-- (courses 테이블에는 created_at/updated_at 컬럼이 없음)
-- 1) 무료 PUBLISHED
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 1, 'Spring Boot 입문', '스프링 부트로 첫 REST API를 만들어 봅니다. 무료 공개 강의.', 'BEGINNER', '김자바', 'PUBLISHED', NOW(), 0);
SET @course_free = LAST_INSERT_ID();

-- 2) 유료 PUBLISHED — 학생이 결제 완료할 강의
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 3, '실전 MySQL 인덱스 튜닝', '실무 쿼리 최적화와 인덱스 설계. 슬로우 쿼리 잡기.', 'INTERMEDIATE', '김자바', 'PUBLISHED', NOW(), 49000);
SET @course_paid = LAST_INSERT_ID();

-- 3) 유료 PUBLISHED — 학생이 장바구니/대기주문에만 담아둘 강의
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 2, 'React 19 실전', '훅·서스펜스·코드 스플리팅까지 실전 프론트엔드.', 'INTERMEDIATE', '김자바', 'PUBLISHED', NOW(), 39000);
SET @course_cart = LAST_INSERT_ID();

-- 4) 유료 DRAFT (목록 비노출 — 강사 콘솔에서만 보임)
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 4, 'Docker & CI/CD 실전(작성중)', '컨테이너 배포 파이프라인. 아직 작성 중인 초안.', 'ADVANCED', '김자바', 'DRAFT', NULL, 79000);
SET @course_draft = LAST_INSERT_ID();

-- ── 섹션 / 강의(lecture) ────────────────────────────────────
-- [무료 강의] 섹션 1개 + 강의 3개
INSERT INTO sections (course_id, title, order_num) VALUES (@course_free, '오리엔테이션', 1);
SET @sec_free_1 = LAST_INSERT_ID();
INSERT INTO lectures (section_id, title, video_url, order_num, duration_seconds) VALUES
    (@sec_free_1, '강의 소개',      'https://cdn.example.com/free/1.mp4', 1, 300),
    (@sec_free_1, '개발 환경 세팅', 'https://cdn.example.com/free/2.mp4', 2, 600),
    (@sec_free_1, '첫 API 만들기',  'https://cdn.example.com/free/3.mp4', 3, 900);

-- [유료 강의] 섹션 2개 + 강의 4개
INSERT INTO sections (course_id, title, order_num) VALUES (@course_paid, '인덱스 기초', 1);
SET @sec_paid_1 = LAST_INSERT_ID();
INSERT INTO sections (course_id, title, order_num) VALUES (@course_paid, '실전 튜닝', 2);
SET @sec_paid_2 = LAST_INSERT_ID();
INSERT INTO lectures (section_id, title, video_url, order_num, duration_seconds) VALUES
    (@sec_paid_1, 'B-Tree 인덱스 원리', 'https://cdn.example.com/paid/1.mp4', 1, 1200),
    (@sec_paid_1, '복합 인덱스 설계',   'https://cdn.example.com/paid/2.mp4', 2, 1500),
    (@sec_paid_2, '슬로우 쿼리 분석',   'https://cdn.example.com/paid/3.mp4', 1, 1800),
    (@sec_paid_2, '실행 계획 읽기',     'https://cdn.example.com/paid/4.mp4', 2, 1600);

-- 진도/재생/북마크에 쓸 무료 강의 lecture id 확보
SET @lec_free_1 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 1);
SET @lec_free_2 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 2);
SET @lec_free_3 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 3);

-- ── 수강 등록 (무료: 직접 수강) ──────────────────────────────
INSERT INTO enrollments (user_id, course_id, created_at) VALUES (@student_id, @course_free, NOW());
SET @enr_free = LAST_INSERT_ID();

-- ── 진도 + 재생 위치 (학생, 무료 강의: 3/3 완료 → 100%) ─────────
INSERT INTO lecture_progress (enrollment_id, lecture_id, is_completed) VALUES
    (@enr_free, @lec_free_1, 1),
    (@enr_free, @lec_free_2, 1),
    (@enr_free, @lec_free_3, 1);
-- 이어듣기 위치 (마지막 강의 7분 지점)
INSERT INTO playback_positions (enrollment_id, lecture_id, current_time_seconds, created_at, updated_at)
VALUES (@enr_free, @lec_free_3, 420, NOW(), NOW());

-- ── 리뷰 (진도 100% 라 작성 가능) ─────────────────────────────
INSERT INTO reviews (user_id, course_id, rating, comment, created_at) VALUES
    (@student_id, @course_free, 5, '입문용으로 최고예요. 설명이 친절합니다.', NOW());

-- ── 북마크 (학생) ────────────────────────────────────────────
INSERT INTO bookmarks (user_id, lecture_id, time_seconds, memo, created_at) VALUES
    (@student_id, @lec_free_2, 180, '환경 세팅 명령어 부분', NOW());

-- ── 주문 + 결제 + 수강 자동생성 (학생이 유료 강의 결제 완료) ──────
INSERT INTO orders (order_no, user_id, status, total_amount, refunded_amount, paid_at, created_at, updated_at)
VALUES (CONCAT('ORD-', DATE_FORMAT(NOW(), '%Y%m%d'), '-0001'), @student_id, 'PAID', 49000, 0, NOW(), NOW(), NOW());
SET @order_paid = LAST_INSERT_ID();
INSERT INTO order_items (order_id, course_id, course_title_snapshot, price_snapshot, status) VALUES
    (@order_paid, @course_paid, '실전 MySQL 인덱스 튜닝', 49000, 'ACTIVE');
INSERT INTO payments (order_id, method, status, amount, mock_transaction_id, created_at) VALUES
    (@order_paid, 'TOSS', 'SUCCESS', 49000, 'MOCK-SEED-0001', NOW());
-- 결제 완료 → 수강 자동 생성 (실제 서비스 흐름 반영)
INSERT INTO enrollments (user_id, course_id, created_at) VALUES (@student_id, @course_paid, NOW());

-- ── 장바구니 (학생이 React 강의 담아둠) ───────────────────────
INSERT INTO cart_items (user_id, course_id, created_at) VALUES (@student_id, @course_cart, NOW());

-- ── 결제 대기 주문 PENDING (학생, React 강의 — 결제 진행 전) ────
INSERT INTO orders (order_no, user_id, status, total_amount, refunded_amount, paid_at, created_at, updated_at)
VALUES (CONCAT('ORD-', DATE_FORMAT(NOW(), '%Y%m%d'), '-0002'), @student_id, 'PENDING', 39000, 0, NULL, NOW(), NOW());
SET @order_pending = LAST_INSERT_ID();
INSERT INTO order_items (order_id, course_id, course_title_snapshot, price_snapshot, status) VALUES
    (@order_pending, @course_cart, 'React 19 실전', 39000, 'ACTIVE');

-- ── Q&A (강의 단위: 학생 질문 → 강사 답변) ─────────────────────
INSERT INTO qna_questions (course_id, author_id, title, content, is_private, answer_count, created_at, updated_at)
VALUES (@course_free, @student_id, '환경 변수 설정이 안 돼요', 'JAVA_HOME 설정 후에도 인식이 안 됩니다. 어떻게 하나요?', 0, 1, NOW(), NOW());
SET @qna_q1 = LAST_INSERT_ID();
INSERT INTO qna_answers (question_id, author_id, author_role, content, created_at, updated_at)
VALUES (@qna_q1, @instructor_id, 'INSTRUCTOR', '터미널을 재시작하거나 `source ~/.zshrc`로 적용해 보세요. 그래도 안 되면 PATH를 확인합니다.', NOW(), NOW());

-- 답변 없는 비공개 질문 1건
INSERT INTO qna_questions (course_id, author_id, title, content, is_private, answer_count, created_at, updated_at)
VALUES (@course_free, @student_id, '결제 영수증 문의(비공개)', '개인 결제 관련 비공개 문의입니다.', 1, 0, NOW(), NOW());

-- ── 신고(Report) — 강사가 학생 리뷰를 신고 1건 PENDING (모더레이션 시연) ──
-- (본인 글은 신고 불가하므로 강사가 신고자)
SET @review_id = (SELECT id FROM reviews WHERE user_id = @student_id AND course_id = @course_free LIMIT 1);
INSERT INTO reports (reporter_id, target_type, target_id, reason, status, resolver_id, resolver_note, created_at, updated_at)
VALUES (@instructor_id, 'REVIEW', @review_id, '내용 검토 요청(시연용 신고).', 'PENDING', NULL, NULL, NOW(), NOW());

-- ── 알림 아웃박스 (디스패처가 처리할 PENDING 1건) ───────────────
INSERT INTO notification_outbox
    (dedup_key, event_type, title, message, channel, status, attempt_count, next_attempt_at, last_error, created_at, sent_at)
VALUES
    (CONCAT('seed-enroll:', @student_id, ':', @course_paid), 'NEW_ENROLLMENT', '신규 수강·결제',
     '실전 MySQL 인덱스 튜닝 강의에 새 수강생이 결제로 등록되었습니다.', 'DISCORD', 'PENDING', 0, NOW(), NULL, NOW(), NULL);

-- ============================================================
-- (선택) 초기화 — 재실행 전 mock data 싹 지우기 (users 제외)
-- 주의: 운영 DB에서 실행 금지!
-- ============================================================
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE notification_outbox; TRUNCATE reports;
-- TRUNCATE qna_answers; TRUNCATE qna_questions;
-- TRUNCATE refunds; TRUNCATE payments; TRUNCATE order_items; TRUNCATE orders;
-- TRUNCATE cart_items; TRUNCATE bookmarks; TRUNCATE playback_positions;
-- TRUNCATE reviews; TRUNCATE lecture_progress; TRUNCATE enrollments;
-- TRUNCATE lectures; TRUNCATE sections; TRUNCATE courses;
-- SET FOREIGN_KEY_CHECKS = 1;
