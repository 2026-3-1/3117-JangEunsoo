package com.jes.devlearn.domain.admin.dto.response;

import com.jes.devlearn.domain.order.dto.response.OrderItemResponse;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        String orderNo,
        Long userId,
        String username,
        OrderStatus status,
        Long totalAmount,
        Long refundedAmount,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static AdminOrderResponse of(Order order, String username, List<OrderItemResponse> items) {
        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                username,
                order.getStatus(),
                order.getTotalAmount(),
                order.getRefundedAmount(),
                order.getPaidAt(),
                order.getCreatedAt(),
                items
        );
    }
}
