package com.jes.devlearn.domain.payment.dto.request;

import com.jes.devlearn.domain.payment.entity.RefundReason;

import java.util.List;

public record RefundRequest(
        List<Long> orderItemIds,
        RefundReason reason
) {
}
