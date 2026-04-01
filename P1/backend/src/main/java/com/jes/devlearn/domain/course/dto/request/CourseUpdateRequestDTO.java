package com.jes.devlearn.domain.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CourseUpdateRequestDTO(
        @NotNull Long categoryId,
        @NotBlank String title,
        String description,
        @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)$", message = "difficulty는 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다.")
        String difficulty,
        String instructorName
) {}
