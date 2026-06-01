package com.jes.devlearn.domain.qna.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.qna.dto.request.QnaAnswerRequest;
import com.jes.devlearn.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.jes.devlearn.domain.qna.entity.QnaQuestion;
import com.jes.devlearn.domain.qna.error.QnaErrorCode;
import com.jes.devlearn.domain.qna.repository.QnaAnswerRepository;
import com.jes.devlearn.domain.qna.repository.QnaQuestionRepository;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Q&A 권한 규칙")
class QnaServiceTest {

    @Mock private QnaQuestionRepository questionRepository;
    @Mock private QnaAnswerRepository answerRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private com.jes.devlearn.domain.user.repository.UserRepository userRepository;
    @Mock private com.jes.devlearn.domain.notification.service.NotificationService notificationService;

    @InjectMocks
    private QnaService qnaService;

    @Test
    @DisplayName("미수강생이 질문 작성 시 NOT_ENROLLED(403)")
    void non_enrolled_cannot_ask() {
        long userId = 5L, courseId = 10L;
        Course course = course(courseId, 99L);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> qnaService.createQuestion(userId, courseId,
                new QnaQuestionCreateRequest("제목", "내용", false)))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(QnaErrorCode.NOT_ENROLLED));
    }

    @Test
    @DisplayName("강의 강사가 아닌 STUDENT가 답변 시 NOT_ANSWERABLE(403)")
    void student_cannot_answer() throws Exception {
        long studentId = 5L, courseId = 10L, questionId = 1L;
        Course course = course(courseId, 99L); // 강사는 99번
        QnaQuestion q = new QnaQuestion(courseId, 7L, "t", "c", false);
        setId(q, questionId);

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> qnaService.createAnswer(studentId, Role.STUDENT, questionId,
                new QnaAnswerRequest("답변")))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(QnaErrorCode.NOT_ANSWERABLE));
    }

    @Test
    @DisplayName("강의 강사가 답변 시 성공, answerCount 증가")
    void instructor_can_answer() throws Exception {
        long instructorId = 99L, courseId = 10L, questionId = 1L;
        Course course = course(courseId, instructorId);
        QnaQuestion q = new QnaQuestion(courseId, 7L, "t", "c", false);
        setId(q, questionId);

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(q.getAnswerCount()).isZero();
        qnaService.createAnswer(instructorId, Role.INSTRUCTOR, questionId, new QnaAnswerRequest("답변입니다"));
        assertThat(q.getAnswerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자는 강사가 아니어도 답변 가능")
    void admin_can_answer() throws Exception {
        long adminId = 1L, courseId = 10L, questionId = 1L;
        Course course = course(courseId, 99L);
        QnaQuestion q = new QnaQuestion(courseId, 7L, "t", "c", false);
        setId(q, questionId);

        when(questionRepository.findById(questionId)).thenReturn(Optional.of(q));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(any())).thenReturn(Optional.empty());

        qnaService.createAnswer(adminId, Role.ADMIN, questionId, new QnaAnswerRequest("관리자 답변"));
        assertThat(q.getAnswerCount()).isEqualTo(1);
    }

    private Course course(Long id, Long instructorId) {
        Course c = new Course(instructorId, 1L, "title", "desc", "초급", null, 0L);
        try {
            Field f = Course.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    private void setId(QnaQuestion q, Long id) throws Exception {
        Field f = QnaQuestion.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(q, id);
    }
}
