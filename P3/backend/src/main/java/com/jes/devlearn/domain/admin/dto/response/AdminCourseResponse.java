package com.jes.devlearn.domain.admin.dto.response;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;

import java.time.LocalDateTime;

public record AdminCourseResponse(
        Long id,
        String title,
        Long instructorId,
        String instructorUsername,
        PublishStatus publishStatus,
        Long price,
        boolean blocked,
        String blockedReason,
        long enrollmentCount,
        LocalDateTime publishedAt
) {
    public static AdminCourseResponse of(Course course, String instructorUsername, long enrollmentCount) {
        return new AdminCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getInstructorId(),
                instructorUsername,
                course.getPublishStatus(),
                course.getPrice(),
                course.isBlocked(),
                course.getBlockedReason(),
                enrollmentCount,
                course.getPublishedAt()
        );
    }
}
