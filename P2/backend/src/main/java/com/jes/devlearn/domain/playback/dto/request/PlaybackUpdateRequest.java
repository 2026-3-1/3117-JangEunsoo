package com.jes.devlearn.domain.playback.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PlaybackUpdateRequest(
        @NotNull Long enrollmentId,
        @NotNull Long lectureId,
        @NotNull @PositiveOrZero Integer currentTimeSeconds
) {
}
