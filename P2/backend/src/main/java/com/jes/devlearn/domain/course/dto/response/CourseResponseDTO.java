package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Course;

public record CourseResponseDTO(
        Long id,
        Long instructorId,
        Long categoryId,
        String title,
        String description,
        String difficulty,
        String instructorName,
        Long price
) {
    public static CourseResponseDTO from(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                course.getDescription(),
                course.getDifficulty(),
                course.getInstructorName(),
                course.getPrice()
        );
    }
}
