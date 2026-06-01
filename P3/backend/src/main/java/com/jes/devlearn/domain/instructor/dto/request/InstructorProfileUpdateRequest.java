package com.jes.devlearn.domain.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InstructorProfileUpdateRequest(
        @NotBlank @Size(max = 50) String displayName,
        String bio,
        @PositiveOrZero Integer careerYears,
        @Size(max = 500) String profileImageUrl
) {
}
