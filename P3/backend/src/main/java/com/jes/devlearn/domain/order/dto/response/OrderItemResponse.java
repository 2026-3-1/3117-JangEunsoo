package com.jes.devlearn.domain.order.dto.response;

import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderItemStatus;

public record OrderItemResponse(
        Long id,
        Long courseId,
        String courseTitle,
        Long price,
        OrderItemStatus status
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getCourseId(),
                item.getCourseTitleSnapshot(),
                item.getPriceSnapshot(),
                item.getStatus()
        );
    }
}
