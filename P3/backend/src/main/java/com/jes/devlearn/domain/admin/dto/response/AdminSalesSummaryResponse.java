package com.jes.devlearn.domain.admin.dto.response;

public record AdminSalesSummaryResponse(
        long grossRevenue,      // 결제 완료된 주문 총액 (환불 포함 전)
        long refundedAmount,    // 누적 환불액
        long netRevenue,        // grossRevenue - refundedAmount
        long paidOrderCount,
        long refundedOrderCount,
        long partialRefundedOrderCount
) {
}
