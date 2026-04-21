package com.jes.devlearn.domain.progress.dto.request;

import jakarta.validation.constraints.NotNull;

public record CompleteRequestDTO(
        @NotNull Long enrollmentId,
        @NotNull Long lectureId
) {}
