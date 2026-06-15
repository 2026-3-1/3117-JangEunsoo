-- ============================================================
-- DevLearn P3 — Mock Data (English / demo & dev)
-- ============================================================
-- Prerequisites:
--   * Tables already created by app startup (ddl-auto=update)
--   * users already exist (this script does NOT create accounts)
--     - user id 1 = STUDENT
--     - user id 2 = INSTRUCTOR
--
-- Usage:
--   mysql -u root -p devlearn < seed-p3-mock-en.sql
--
-- This script RESETS mock tables first (users untouched), then
-- re-inserts in English. Safe to re-run. DO NOT run on production.
-- ============================================================

-- ── Reset mock data (users excluded) ────────────────────────
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE notification_outbox;
TRUNCATE reports;
TRUNCATE qna_answers;
TRUNCATE qna_questions;
TRUNCATE refunds;
TRUNCATE payments;
TRUNCATE order_items;
TRUNCATE orders;
TRUNCATE cart_items;
TRUNCATE bookmarks;
TRUNCATE playback_positions;
TRUNCATE reviews;
TRUNCATE lecture_progress;
TRUNCATE enrollments;
TRUNCATE lectures;
TRUNCATE sections;
TRUNCATE courses;
SET FOREIGN_KEY_CHECKS = 1;

-- ── Target user ids ─────────────────────────────────────────
SET @instructor_id = 2;   -- instructor
SET @student_id    = 1;   -- student (learner)

-- Create instructor profile if missing
INSERT IGNORE INTO instructor_profiles (user_id, display_name, bio, career_years, profile_image_url, created_at, updated_at)
VALUES (@instructor_id, 'James Kim', '10-year backend engineer. Spring & MySQL specialist.', 10, NULL, NOW(), NOW());

-- ── Categories ──────────────────────────────────────────────
INSERT IGNORE INTO categories (id, name) VALUES
    (1, 'Backend'),
    (2, 'Frontend'),
    (3, 'Database'),
    (4, 'DevOps'),
    (5, 'Computer Science');

-- ── Courses (1 free / 2 paid / 1 draft) ─────────────────────
-- (courses table has no created_at/updated_at columns)
-- 1) Free, PUBLISHED
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 1, 'Spring Boot Basics', 'Build your first REST API with Spring Boot. Free public course.', 'BEGINNER', 'James Kim', 'PUBLISHED', NOW(), 0);
SET @course_free = LAST_INSERT_ID();

-- 2) Paid, PUBLISHED — student completes payment for this one
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 3, 'Practical MySQL Index Tuning', 'Real-world query optimization and index design. Catch slow queries.', 'INTERMEDIATE', 'James Kim', 'PUBLISHED', NOW(), 49000);
SET @course_paid = LAST_INSERT_ID();

-- 3) Paid, PUBLISHED — only added to cart / pending order by student
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 2, 'React 19 in Practice', 'Hooks, Suspense, and code splitting for real-world frontend.', 'INTERMEDIATE', 'James Kim', 'PUBLISHED', NOW(), 39000);
SET @course_cart = LAST_INSERT_ID();

-- 4) Paid, DRAFT (hidden from listing — visible only in instructor console)
INSERT INTO courses
    (instructor_id, category_id, title, description, difficulty, instructor_name, publish_status, published_at, price)
VALUES
    (@instructor_id, 4, 'Docker & CI/CD in Practice (WIP)', 'Container deployment pipeline. Draft still in progress.', 'ADVANCED', 'James Kim', 'DRAFT', NULL, 79000);
SET @course_draft = LAST_INSERT_ID();

-- ── Sections / Lectures ─────────────────────────────────────
-- [Free course] 1 section + 3 lectures
INSERT INTO sections (course_id, title, order_num) VALUES (@course_free, 'Orientation', 1);
SET @sec_free_1 = LAST_INSERT_ID();
INSERT INTO lectures (section_id, title, video_url, order_num, duration_seconds) VALUES
    (@sec_free_1, 'Course Introduction',    'https://www.youtube.com/watch?v=9SGDpanrc8U', 1, 300),
    (@sec_free_1, 'Dev Environment Setup',  'https://www.youtube.com/watch?v=gJrjgg1KVL4', 2, 600),
    (@sec_free_1, 'Building Your First API','https://www.youtube.com/watch?v=vtPkZShrvXQ', 3, 900);

-- [Paid course] 2 sections + 4 lectures
INSERT INTO sections (course_id, title, order_num) VALUES (@course_paid, 'Index Fundamentals', 1);
SET @sec_paid_1 = LAST_INSERT_ID();
INSERT INTO sections (course_id, title, order_num) VALUES (@course_paid, 'Practical Tuning', 2);
SET @sec_paid_2 = LAST_INSERT_ID();
INSERT INTO lectures (section_id, title, video_url, order_num, duration_seconds) VALUES
    (@sec_paid_1, 'How B-Tree Indexes Work', 'https://www.youtube.com/watch?v=aZjYr87r1b8', 1, 1200),
    (@sec_paid_1, 'Composite Index Design',  'https://www.youtube.com/watch?v=ELR7-RdU9XU', 2, 1500),
    (@sec_paid_2, 'Analyzing Slow Queries',  'https://www.youtube.com/watch?v=BHwzDmr6d7s', 1, 1800),
    (@sec_paid_2, 'Reading the Query Plan',   'https://www.youtube.com/watch?v=fGdGl43et0Y', 2, 1600);

-- Capture free-course lecture ids for progress/playback/bookmark
SET @lec_free_1 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 1);
SET @lec_free_2 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 2);
SET @lec_free_3 = (SELECT id FROM lectures WHERE section_id = @sec_free_1 AND order_num = 3);

-- ── Enrollment (free: direct) ───────────────────────────────
INSERT INTO enrollments (user_id, course_id, created_at) VALUES (@student_id, @course_free, NOW());
SET @enr_free = LAST_INSERT_ID();

-- ── Progress + playback (student, free course: 3/3 done → 100%) ─
INSERT INTO lecture_progress (enrollment_id, lecture_id, is_completed) VALUES
    (@enr_free, @lec_free_1, 1),
    (@enr_free, @lec_free_2, 1),
    (@enr_free, @lec_free_3, 1);
-- Resume position (last lecture at 7:00)
INSERT INTO playback_positions (enrollment_id, lecture_id, current_time_seconds, created_at, updated_at)
VALUES (@enr_free, @lec_free_3, 420, NOW(), NOW());

-- ── Review (allowed since progress is 100%) ─────────────────
INSERT INTO reviews (user_id, course_id, rating, comment, created_at) VALUES
    (@student_id, @course_free, 5, 'Best beginner course. Very clear explanations.', NOW());

-- ── Bookmark (student) ──────────────────────────────────────
INSERT INTO bookmarks (user_id, lecture_id, time_seconds, memo, created_at) VALUES
    (@student_id, @lec_free_2, 180, 'Environment setup commands part', NOW());

-- ── Order + payment + auto enrollment (student paid for paid course) ─
INSERT INTO orders (order_no, user_id, status, total_amount, refunded_amount, paid_at, created_at, updated_at)
VALUES (CONCAT('ORD-', DATE_FORMAT(NOW(), '%Y%m%d'), '-0001'), @student_id, 'PAID', 49000, 0, NOW(), NOW(), NOW());
SET @order_paid = LAST_INSERT_ID();
INSERT INTO order_items (order_id, course_id, course_title_snapshot, price_snapshot, status) VALUES
    (@order_paid, @course_paid, 'Practical MySQL Index Tuning', 49000, 'ACTIVE');
INSERT INTO payments (order_id, method, status, amount, mock_transaction_id, created_at) VALUES
    (@order_paid, 'TOSS', 'SUCCESS', 49000, 'MOCK-SEED-0001', NOW());
-- Payment success → auto-create enrollment (mirrors real service flow)
INSERT INTO enrollments (user_id, course_id, created_at) VALUES (@student_id, @course_paid, NOW());

-- ── Cart (student added the React course) ───────────────────
INSERT INTO cart_items (user_id, course_id, created_at) VALUES (@student_id, @course_cart, NOW());

-- ── Pending order (student, React course — not yet paid) ────
INSERT INTO orders (order_no, user_id, status, total_amount, refunded_amount, paid_at, created_at, updated_at)
VALUES (CONCAT('ORD-', DATE_FORMAT(NOW(), '%Y%m%d'), '-0002'), @student_id, 'PENDING', 39000, 0, NULL, NOW(), NOW());
SET @order_pending = LAST_INSERT_ID();
INSERT INTO order_items (order_id, course_id, course_title_snapshot, price_snapshot, status) VALUES
    (@order_pending, @course_cart, 'React 19 in Practice', 39000, 'ACTIVE');

-- ── Q&A (course-scoped: student asks → instructor answers) ──
INSERT INTO qna_questions (course_id, author_id, title, content, is_private, answer_count, created_at, updated_at)
VALUES (@course_free, @student_id, 'Environment variables not recognized', 'After setting JAVA_HOME it is still not recognized. What should I do?', 0, 1, NOW(), NOW());
SET @qna_q1 = LAST_INSERT_ID();
INSERT INTO qna_answers (question_id, author_id, author_role, content, created_at, updated_at)
VALUES (@qna_q1, @instructor_id, 'INSTRUCTOR', 'Restart your terminal or run `source ~/.zshrc`. If it still fails, double-check your PATH.', NOW(), NOW());

-- One unanswered private question
INSERT INTO qna_questions (course_id, author_id, title, content, is_private, answer_count, created_at, updated_at)
VALUES (@course_free, @student_id, 'Receipt inquiry (private)', 'A private inquiry about my payment receipt.', 1, 0, NOW(), NOW());

-- ── Report — instructor reports the student review, PENDING (moderation demo) ──
-- (cannot report your own content, so the instructor is the reporter)
SET @review_id = (SELECT id FROM reviews WHERE user_id = @student_id AND course_id = @course_free LIMIT 1);
INSERT INTO reports (reporter_id, target_type, target_id, reason, status, resolver_id, resolver_note, created_at, updated_at)
VALUES (@instructor_id, 'REVIEW', @review_id, 'Requesting content review (demo report).', 'PENDING', NULL, NULL, NOW(), NOW());

-- ── Notification outbox (one PENDING row for the dispatcher) ──
INSERT INTO notification_outbox
    (dedup_key, event_type, title, message, channel, status, attempt_count, next_attempt_at, last_error, created_at, sent_at)
VALUES
    (CONCAT('seed-enroll:', @student_id, ':', @course_paid), 'NEW_ENROLLMENT', 'New Enrollment / Payment',
     'A new learner enrolled in "Practical MySQL Index Tuning" via payment.', 'DISCORD', 'PENDING', 0, NOW(), NULL, NOW(), NULL);
