package com.jes.devlearn.domain.payment.dto.response;

import com.jes.devlearn.domain.payment.entity.Payment;
import com.jes.devlearn.domain.payment.entity.PaymentMethod;
import com.jes.devlearn.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentMethod method,
        PaymentStatus status,
        Long amount,
        String mockTransactionId,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getMethod(),
                p.getStatus(),
                p.getAmount(),
                p.getMockTransactionId(),
                p.getCreatedAt()
        );
    }
}
