package com.jes.devlearn.domain.instructor.dto.response;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.instructor.entity.InstructorProfile;

import java.util.List;

public record InstructorPublicProfileResponse(
        Long userId,
        String username,
        String displayName,
        String bio,
        Integer careerYears,
        String profileImageUrl,
        long totalEnrollments,
        long totalReviews,
        double averageRating,
        List<PublicCourseItem> courses
) {
    public record PublicCourseItem(
            Long id,
            String title,
            String description,
            String difficulty,
            Long price,
            long enrollmentCount,
            double averageRating
    ) {
        public static PublicCourseItem from(Course course, long enrollmentCount, double averageRating) {
            return new PublicCourseItem(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getDifficulty(),
                    course.getPrice(),
                    enrollmentCount,
                    averageRating
            );
        }
    }

    public static InstructorPublicProfileResponse of(
            InstructorProfile profile,
            long totalEnrollments,
            long totalReviews,
            double averageRating,
            List<PublicCourseItem> courses
    ) {
        return new InstructorPublicProfileResponse(
                profile.getUser().getId(),
                profile.getUser().getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getCareerYears(),
                profile.getProfileImageUrl(),
                totalEnrollments,
                totalReviews,
                averageRating,
                courses
        );
    }
}
