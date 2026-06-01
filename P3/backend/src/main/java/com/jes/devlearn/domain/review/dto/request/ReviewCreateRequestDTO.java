package com.jes.devlearn.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequestDTO(
        @NotNull Long courseId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String comment
) {}
