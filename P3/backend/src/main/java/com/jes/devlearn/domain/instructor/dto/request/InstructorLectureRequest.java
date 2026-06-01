package com.jes.devlearn.domain.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record InstructorLectureRequest(
        @NotBlank String title,
        String videoUrl,
        Integer orderNum,
        @PositiveOrZero Integer durationSeconds
) {
}
