package com.jes.devlearn.domain.instructor.service;

import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderItemStatus;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.domain.payment.service.RefundService;
import com.jes.devlearn.global.security.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorCourseCancellationService {

    private final OwnershipValidator ownershipValidator;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final RefundService refundService;

    @Transactional
    public void cancelCourse(Long instructorId, Long courseId) {
        Course course = ownershipValidator.requireOwnedCourse(courseId, instructorId);

        List<OrderItem> activeItems = orderItemRepository.findAllByCourseIdAndStatus(courseId, OrderItemStatus.ACTIVE);
        Set<Long> orderIds = activeItems.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());

        for (Long orderId : orderIds) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PARTIAL_REFUNDED) {
                    List<Long> targetItemIds = activeItems.stream()
                            .filter(it -> it.getOrderId().equals(orderId))
                            .map(OrderItem::getId)
                            .toList();
                    if (!targetItemIds.isEmpty()) {
                        refundService.refund(order.getUserId(), orderId, targetItemIds, com.jes.devlearn.domain.payment.entity.RefundReason.COURSE_CANCELLED);
                    }
                }
            });
        }

        course.archive();
    }
}
