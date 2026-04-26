package com.jes.devlearn.domain.enrollment.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.dto.request.EnrollmentCreateRequestDTO;
import com.jes.devlearn.domain.enrollment.error.EnrollmentErrorCode;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("/api/enrollments — 유료 강의 직접 enroll 차단")
class EnrollmentPaidGuardTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("유료 강의에 직접 enroll 시도 → 400 COURSE_NOT_FREE")
    void paid_course_direct_enroll_returns_400() throws Exception {
        Course course = new Course(1L, 1L, "유료 강의", "desc", "EASY", "강사", 30000L);
        setId(course, 10L);
        setPublishStatus(course, PublishStatus.PUBLISHED);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() ->
                enrollmentService.enroll(5L, new EnrollmentCreateRequestDTO(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(EnrollmentErrorCode.COURSE_NOT_FREE);
    }

    private void setId(Course course, Long id) throws Exception {
        Field f = Course.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(course, id);
    }

    private void setPublishStatus(Course course, PublishStatus status) throws Exception {
        Field f = Course.class.getDeclaredField("publishStatus");
        f.setAccessible(true);
        f.set(course, status);
    }
}
