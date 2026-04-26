package com.jes.devlearn.domain.review.service;

import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.course.repository.LectureRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.progress.repository.LectureProgressRepository;
import com.jes.devlearn.domain.review.dto.request.ReviewCreateRequestDTO;
import com.jes.devlearn.domain.review.error.ReviewProgressGateException;
import com.jes.devlearn.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리뷰 진도 게이트 - 80% 미만이면 422")
class ReviewProgressGateTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private LectureProgressRepository lectureProgressRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("진도율 79% — ReviewProgressGateException 발생, body에 currentProgressRate=79")
    void progress_79_throws_gate_exception() throws Exception {
        long userId = 5L;
        long courseId = 10L;
        Enrollment enrollment = new Enrollment(userId, courseId);
        setId(enrollment, 100L);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(enrollmentRepository.findAllByUserId(userId)).thenReturn(List.of(enrollment));
        when(lectureRepository.countByCourseId(courseId)).thenReturn(100L);
        when(lectureProgressRepository.countByEnrollmentId(100L)).thenReturn(79L);

        assertThatThrownBy(() ->
                reviewService.createReview(userId, new ReviewCreateRequestDTO(courseId, 5, "good")))
                .isInstanceOf(ReviewProgressGateException.class)
                .satisfies(ex -> {
                    ReviewProgressGateException g = (ReviewProgressGateException) ex;
                    assertThat(g.getCurrentProgressRate()).isEqualTo(79);
                    assertThat(g.getRequiredRate()).isEqualTo(80);
                });
    }

    @Test
    @DisplayName("진도율 80% — 통과")
    void progress_80_passes() throws Exception {
        long userId = 5L;
        long courseId = 10L;
        Enrollment enrollment = new Enrollment(userId, courseId);
        setId(enrollment, 100L);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(enrollmentRepository.findAllByUserId(userId)).thenReturn(List.of(enrollment));
        when(lectureRepository.countByCourseId(courseId)).thenReturn(10L);
        when(lectureProgressRepository.countByEnrollmentId(100L)).thenReturn(8L);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 게이트 통과해야 NPE 없이 끝남
        assertThat(reviewService.createReview(userId, new ReviewCreateRequestDTO(courseId, 5, "good")))
                .isNotNull();
    }

    private void setId(Enrollment enrollment, Long id) throws Exception {
        Field f = Enrollment.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(enrollment, id);
    }
}
