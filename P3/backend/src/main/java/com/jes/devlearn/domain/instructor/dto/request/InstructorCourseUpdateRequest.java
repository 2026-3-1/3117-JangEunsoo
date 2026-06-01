package com.jes.devlearn.domain.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;

public record InstructorCourseUpdateRequest(
        @NotNull Long categoryId,
        @NotBlank String title,
        String description,
        @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)$",
                message = "difficulty는 BEGINNER, INTERMEDIATE, ADVANCED 중 하나여야 합니다.")
        String difficulty,
        @PositiveOrZero(message = "price는 0 이상이어야 합니다.")
        Long price
) {
}
