package com.jes.devlearn.global.security;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {
    private final CourseRepository courseRepository;

    public Course requireOwnedCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.isOwnedBy(userId)) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }
}
