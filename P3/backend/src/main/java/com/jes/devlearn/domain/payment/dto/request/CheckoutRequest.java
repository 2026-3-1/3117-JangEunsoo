package com.jes.devlearn.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long orderId
) {
}
