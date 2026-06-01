package com.jes.devlearn.domain.order.dto.response;

import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        OrderStatus status,
        Long totalAmount,
        Long refundedAmount,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse of(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRefundedAmount(),
                order.getPaidAt(),
                order.getCreatedAt(),
                items
        );
    }
}
