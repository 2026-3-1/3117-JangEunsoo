package com.jes.devlearn.domain.cart.dto.request;

import jakarta.validation.constraints.NotNull;

public record CartAddRequest(
        @NotNull Long courseId
) {
}
