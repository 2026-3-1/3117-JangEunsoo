package com.jes.devlearn.global.security;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnershipValidator - 소유자 검증")
class OwnershipValidatorTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private OwnershipValidator ownershipValidator;

    @Test
    @DisplayName("다른 강사의 강의 접근 시 COURSE_NOT_FOUND(404)를 던진다 (info-leak 방지)")
    void other_instructor_course_access_returns_404() throws Exception {
        Course course = new Course(1L, 1L, "남의 강의", "desc", "EASY", "김강사");
        setId(course, 42L);
        when(courseRepository.findById(42L)).thenReturn(Optional.of(course));

        Long attackerUserId = 99L;

        assertThatThrownBy(() -> ownershipValidator.requireOwnedCourse(42L, attackerUserId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 강의도 동일하게 COURSE_NOT_FOUND(404)")
    void missing_course_returns_404() {
        when(courseRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownershipValidator.requireOwnedCourse(42L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("소유자면 Course 반환")
    void owner_returns_course() throws Exception {
        Course course = new Course(7L, 1L, "내 강의", "desc", "EASY", "김강사");
        setId(course, 10L);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Course result = ownershipValidator.requireOwnedCourse(10L, 7L);

        assertThat(result).isSameAs(course);
    }

    private void setId(Course course, Long id) throws Exception {
        Field f = Course.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(course, id);
    }
}
