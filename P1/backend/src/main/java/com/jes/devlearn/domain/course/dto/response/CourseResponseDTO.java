package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Course;

public record CourseResponseDTO(
        Long id,
        Long categoryId,
        String title
) {
    public static CourseResponseDTO from(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getCategoryId(),
                course.getTitle()
        );
    }
}