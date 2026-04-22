package com.jes.devlearn.domain.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InstructorSectionRequest(
        @NotBlank String title,
        Integer orderNum
) {
}
