package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Course;

import java.util.List;

public record CourseDetailResponseDTO(
        Long id,
        Long categoryId,
        String title,
        String description,
        String difficulty,
        String instructorName,
        List<SectionResponseDTO> sections
) {
    public static CourseDetailResponseDTO from(Course course, List<SectionResponseDTO> sections) {
        return new CourseDetailResponseDTO(
                course.getId(),
                course.getCategoryId(),
                course.getTitle(),
                course.getDescription(),
                course.getDifficulty(),
                course.getInstructorName(),
                sections
        );
    }
}
