package com.jes.devlearn.domain.bookmark.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record BookmarkUpdateRequest(
        @PositiveOrZero Integer timeSeconds,
        String memo
) {
}
