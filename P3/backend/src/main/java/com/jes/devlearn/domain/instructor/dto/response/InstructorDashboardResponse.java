package com.jes.devlearn.domain.instructor.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record InstructorDashboardResponse(
        long totalCourses,
        long publishedCourses,
        long draftCourses,
        long archivedCourses,
        long totalEnrollments,
        long totalReviews,
        double averageRating,
        List<RecentEnrollmentItem> recentEnrollments
) {
    public record RecentEnrollmentItem(
            Long enrollmentId,
            Long courseId,
            String courseTitle,
            Long studentId,
            String studentUsername,
            LocalDateTime enrolledAt
    ) {}
}
