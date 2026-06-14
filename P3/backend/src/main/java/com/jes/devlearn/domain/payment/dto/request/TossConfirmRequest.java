package com.jes.devlearn.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Toss 결제 성공 리다이렉트 후 프론트가 전달하는 승인 요청.
 * amount는 위변조 검증용 — 서버가 order_items.price_snapshot 합으로 재계산해 대조한다.
 */
public record TossConfirmRequest(
        @NotNull Long orderId,
        @NotBlank String paymentKey,
        @NotNull Long amount
) {
}
