package com.jes.devlearn.domain.payment.service;

import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderItemStatus;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.error.OrderErrorCode;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.domain.order.service.OrderService;
import com.jes.devlearn.domain.payment.entity.Refund;
import com.jes.devlearn.domain.payment.entity.RefundReason;
import com.jes.devlearn.domain.payment.repository.RefundRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRepository refundRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderService orderService;

    @Transactional
    public OrderResponse refund(Long userId, Long orderId, List<Long> orderItemIds, RefundReason reason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_REFUNDABLE);
        }

        return performRefund(order, orderItemIds, reason);
    }

    @Transactional
    public OrderResponse refundByAdmin(Long orderId, List<Long> orderItemIds, RefundReason reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_REFUNDABLE);
        }
        return performRefund(order, orderItemIds, reason == null ? RefundReason.OTHER : reason);
    }

    @Transactional
    public void refundForCourseCancellation(Order order) {
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            return;
        }
        performRefund(order, null, RefundReason.COURSE_CANCELLED);
    }

    private OrderResponse performRefund(Order order, List<Long> orderItemIds, RefundReason reason) {
        List<OrderItem> allItems = orderItemRepository.findAllByOrderId(order.getId());
        Set<Long> targetIds;
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            targetIds = new HashSet<>();
            for (OrderItem it : allItems) {
                if (it.isActive()) targetIds.add(it.getId());
            }
        } else {
            targetIds = new HashSet<>(orderItemIds);
        }
        if (targetIds.isEmpty()) {
            throw new CustomException(OrderErrorCode.INVALID_REFUND_ITEMS);
        }

        long refundedAmount = 0L;
        for (OrderItem item : allItems) {
            if (!targetIds.contains(item.getId())) continue;
            if (!item.isActive()) {
                throw new CustomException(OrderErrorCode.INVALID_REFUND_ITEMS);
            }
            item.markRefunded();
            refundedAmount += item.getPriceSnapshot();
            refundRepository.save(new Refund(order.getId(), item.getId(), item.getPriceSnapshot(), reason));

            enrollmentRepository.findAll().stream()
                    .filter(e -> e.getUserId().equals(order.getUserId()) && e.getCourseId().equals(item.getCourseId()))
                    .findFirst()
                    .ifPresent(this::deleteEnrollment);
        }

        long activeRemaining = allItems.stream().filter(it -> it.getStatus() == OrderItemStatus.ACTIVE).count();
        order.applyRefund(refundedAmount, activeRemaining == 0);

        return orderService.getMine(order.getUserId(), order.getId());
    }

    private void deleteEnrollment(Enrollment e) {
        enrollmentRepository.delete(e);
    }
}
