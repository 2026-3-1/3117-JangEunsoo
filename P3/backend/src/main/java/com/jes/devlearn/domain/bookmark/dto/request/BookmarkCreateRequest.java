package com.jes.devlearn.domain.bookmark.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookmarkCreateRequest(
        @NotNull Long lectureId,
        @NotNull @PositiveOrZero Integer timeSeconds,
        String memo
) {
}
