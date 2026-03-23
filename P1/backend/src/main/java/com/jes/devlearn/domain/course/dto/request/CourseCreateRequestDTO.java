package com.jes.devlearn.domain.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseCreateRequestDTO(
        @NotNull Long categoryId,
        @NotBlank String title
) {}