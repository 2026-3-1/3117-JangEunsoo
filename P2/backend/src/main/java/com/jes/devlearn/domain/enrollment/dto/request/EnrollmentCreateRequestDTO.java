package com.jes.devlearn.domain.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnrollmentCreateRequestDTO(
        @NotNull Long courseId
) {}
