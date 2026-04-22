package com.jes.devlearn.domain.instructor.dto.response;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;

import java.time.LocalDateTime;

public record InstructorCourseResponse(
        Long id,
        Long instructorId,
        Long categoryId,
        String title,
        String description,
        String difficulty,
        Long price,
        PublishStatus publishStatus,
        LocalDateTime publishedAt
) {
    public static InstructorCourseResponse from(Course course) {
        return new InstructorCourseResponse(
                course.getId(),
                course.getInstructorId(),
                course.getCategoryId(),
                course.getTitle(),
                course.getDescription(),
                course.getDifficulty(),
                course.getPrice(),
                course.getPublishStatus(),
                course.getPublishedAt()
        );
    }
}
