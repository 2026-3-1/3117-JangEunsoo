package com.jes.devlearn.domain.instructor.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record InstructorCourseStudentsResponse(
        Long courseId,
        long totalLectures,
        List<StudentItem> students
) {
    public record StudentItem(
            Long enrollmentId,
            Long userId,
            String username,
            LocalDateTime enrolledAt,
            long completedLectures,
            int progressRate
    ) {}
}
