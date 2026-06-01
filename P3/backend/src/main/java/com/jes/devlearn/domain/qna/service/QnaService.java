package com.jes.devlearn.domain.qna.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.qna.dto.request.QnaAnswerRequest;
import com.jes.devlearn.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.jes.devlearn.domain.qna.dto.request.QnaQuestionUpdateRequest;
import com.jes.devlearn.domain.qna.dto.response.QnaAnswerResponse;
import com.jes.devlearn.domain.qna.dto.response.QnaQuestionPageResponse;
import com.jes.devlearn.domain.qna.dto.response.QnaQuestionResponse;
import com.jes.devlearn.domain.qna.entity.QnaAnswer;
import com.jes.devlearn.domain.qna.entity.QnaQuestion;
import com.jes.devlearn.domain.qna.error.QnaErrorCode;
import com.jes.devlearn.domain.qna.repository.QnaAnswerRepository;
import com.jes.devlearn.domain.qna.repository.QnaQuestionRepository;
import com.jes.devlearn.domain.notification.service.NotificationService;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.repository.UserRepository;
import com.jes.devlearn.global.exception.CustomException;
import com.jes.devlearn.global.security.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaQuestionRepository questionRepository;
    private final QnaAnswerRepository answerRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ---------------------------------------------------------------------
    // 질문 작성: 해당 강의 수강생만 (강사 본인/관리자도 수강 시 가능)
    // ---------------------------------------------------------------------
    @Transactional
    public QnaQuestionResponse createQuestion(Long userId, Long courseId, QnaQuestionCreateRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new CustomException(QnaErrorCode.NOT_ENROLLED);
        }
        QnaQuestion q = questionRepository.save(
                new QnaQuestion(courseId, userId,
                        HtmlSanitizer.sanitize(req.title()),
                        HtmlSanitizer.sanitize(req.content()),
                        req.isPrivate()));
        // 강사에게 새 질문 알림
        notificationService.enqueue(
                "qna-q:" + q.getId(), "QNA_QUESTION", "Q&A 새 질문",
                String.format("'%s' 강의에 새 질문이 등록되었습니다: %s (강사 #%s)",
                        course.getTitle(), req.title(), course.getInstructorId()));
        return QnaQuestionResponse.summary(q, usernameOf(userId));
    }

    // ---------------------------------------------------------------------
    // 질문 목록: 강의 단위. 비공개 글은 작성자·강사·관리자만 노출
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public QnaQuestionPageResponse listByCourse(Long userId, Role role, Long courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        boolean privileged = isCourseManager(role, userId, course);
        Page<QnaQuestion> page = questionRepository.findAllByCourseIdOrderByIdDesc(courseId, pageable);

        Map<Long, String> usernameById = new HashMap<>();
        page.getContent().stream().map(QnaQuestion::getAuthorId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> usernameById.put(uid, u.getUsername())));

        List<QnaQuestionResponse> content = page.getContent().stream()
                .map(q -> {
                    boolean canSee = !q.isPrivate() || privileged || q.isAuthoredBy(userId);
                    if (!canSee) {
                        // 비공개 글은 제목/내용을 가린 마스킹 요약으로 반환
                        return new QnaQuestionResponse(
                                q.getId(), q.getCourseId(), q.getAuthorId(), null,
                                "비공개 질문입니다.", null, true, q.isAnswered(),
                                q.getAnswerCount(), q.getCreatedAt(), null);
                    }
                    return QnaQuestionResponse.summary(q, usernameById.get(q.getAuthorId()));
                })
                .toList();

        return new QnaQuestionPageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    // ---------------------------------------------------------------------
    // 질문 상세 + 답변 목록
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public QnaQuestionResponse getQuestion(Long userId, Role role, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.QUESTION_NOT_FOUND));
        Course course = courseRepository.findById(q.getCourseId())
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        boolean privileged = isCourseManager(role, userId, course);
        if (q.isPrivate() && !privileged && !q.isAuthoredBy(userId)) {
            throw new CustomException(QnaErrorCode.QNA_ACCESS_DENIED);
        }

        List<QnaAnswer> answers = answerRepository.findAllByQuestionIdOrderByIdAsc(questionId);
        Map<Long, String> usernameById = new HashMap<>();
        answers.stream().map(QnaAnswer::getAuthorId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> usernameById.put(uid, u.getUsername())));

        List<QnaAnswerResponse> answerDtos = answers.stream()
                .map(a -> QnaAnswerResponse.of(a, usernameById.get(a.getAuthorId())))
                .toList();

        return QnaQuestionResponse.detail(q, usernameOf(q.getAuthorId()), answerDtos);
    }

    // ---------------------------------------------------------------------
    // 질문 수정: 작성자 본인만
    // ---------------------------------------------------------------------
    @Transactional
    public QnaQuestionResponse updateQuestion(Long userId, Long questionId, QnaQuestionUpdateRequest req) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.QUESTION_NOT_FOUND));
        if (!q.isAuthoredBy(userId)) {
            throw new CustomException(QnaErrorCode.NOT_QUESTION_OWNER);
        }
        q.update(HtmlSanitizer.sanitize(req.title()), HtmlSanitizer.sanitize(req.content()), req.isPrivate());
        return QnaQuestionResponse.summary(q, usernameOf(userId));
    }

    // ---------------------------------------------------------------------
    // 질문 삭제: 작성자 / 강사 / 관리자
    // ---------------------------------------------------------------------
    @Transactional
    public void deleteQuestion(Long userId, Role role, Long questionId) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.QUESTION_NOT_FOUND));
        Course course = courseRepository.findById(q.getCourseId())
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!q.isAuthoredBy(userId) && !isCourseManager(role, userId, course)) {
            throw new CustomException(QnaErrorCode.NOT_QUESTION_OWNER);
        }
        answerRepository.deleteAllByQuestionId(questionId);
        questionRepository.delete(q);
    }

    // ---------------------------------------------------------------------
    // 답변 작성: 강사(해당 강의) 또는 관리자
    // ---------------------------------------------------------------------
    @Transactional
    public QnaAnswerResponse createAnswer(Long userId, Role role, Long questionId, QnaAnswerRequest req) {
        QnaQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.QUESTION_NOT_FOUND));
        Course course = courseRepository.findById(q.getCourseId())
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!isCourseManager(role, userId, course)) {
            throw new CustomException(QnaErrorCode.NOT_ANSWERABLE);
        }
        QnaAnswer answer = answerRepository.save(
                new QnaAnswer(questionId, userId, role == null ? "INSTRUCTOR" : role.name(),
                        HtmlSanitizer.sanitize(req.content())));
        q.increaseAnswerCount();
        // 질문 작성자에게 답변 알림
        notificationService.enqueue(
                "qna-a:" + answer.getId(), "QNA_ANSWER", "Q&A 답변 등록",
                String.format("내 질문 '%s'에 답변이 등록되었습니다. (질문자 #%d)",
                        q.getTitle(), q.getAuthorId()));
        return QnaAnswerResponse.of(answer, usernameOf(userId));
    }

    // ---------------------------------------------------------------------
    // 답변 수정: 답변 작성자 / 관리자
    // ---------------------------------------------------------------------
    @Transactional
    public QnaAnswerResponse updateAnswer(Long userId, Role role, Long answerId, QnaAnswerRequest req) {
        QnaAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.ANSWER_NOT_FOUND));
        if (!answer.isAuthoredBy(userId) && role != Role.ADMIN) {
            throw new CustomException(QnaErrorCode.NOT_ANSWER_OWNER);
        }
        answer.update(HtmlSanitizer.sanitize(req.content()));
        return QnaAnswerResponse.of(answer, usernameOf(answer.getAuthorId()));
    }

    // ---------------------------------------------------------------------
    // 답변 삭제: 답변 작성자 / 관리자
    // ---------------------------------------------------------------------
    @Transactional
    public void deleteAnswer(Long userId, Role role, Long answerId) {
        QnaAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(QnaErrorCode.ANSWER_NOT_FOUND));
        if (!answer.isAuthoredBy(userId) && role != Role.ADMIN) {
            throw new CustomException(QnaErrorCode.NOT_ANSWER_OWNER);
        }
        questionRepository.findById(answer.getQuestionId()).ifPresent(QnaQuestion::decreaseAnswerCount);
        answerRepository.delete(answer);
    }

    // ---------------------------------------------------------------------
    // 권한 헬퍼: 강의 강사 본인 또는 관리자
    // ---------------------------------------------------------------------
    private boolean isCourseManager(Role role, Long userId, Course course) {
        if (role == Role.ADMIN) return true;
        return course.isOwnedBy(userId);
    }

    private String usernameOf(Long userId) {
        return userRepository.findById(userId).map(u -> u.getUsername()).orElse(null);
    }
}
