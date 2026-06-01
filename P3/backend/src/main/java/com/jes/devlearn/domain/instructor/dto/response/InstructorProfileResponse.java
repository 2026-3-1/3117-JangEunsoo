package com.jes.devlearn.domain.instructor.dto.response;

import com.jes.devlearn.domain.instructor.entity.InstructorProfile;

public record InstructorProfileResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        String bio,
        Integer careerYears,
        String profileImageUrl
) {
    public static InstructorProfileResponse from(InstructorProfile profile) {
        return new InstructorProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getCareerYears(),
                profile.getProfileImageUrl()
        );
    }
}
