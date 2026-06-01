package com.jes.devlearn.domain.admin.service;

import com.jes.devlearn.domain.admin.dto.response.AdminOrderPageResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminOrderResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminSalesSummaryResponse;
import com.jes.devlearn.domain.order.dto.response.OrderItemResponse;
import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.domain.payment.entity.RefundReason;
import com.jes.devlearn.domain.payment.service.RefundService;
import com.jes.devlearn.domain.user.entity.User;
import com.jes.devlearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RefundService refundService;

    @Transactional(readOnly = true)
    public AdminOrderPageResponse list(OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findAllForAdmin(status, pageable);

        List<Long> orderIds = page.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (OrderItem item : orderItemRepository.findAllByOrderIdIn(orderIds)) {
                itemsByOrder.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }

        Map<Long, String> usernameById = new HashMap<>();
        page.getContent().stream().map(Order::getUserId).distinct().forEach(uid ->
                userRepository.findById(uid).ifPresent(u -> usernameById.put(uid, u.getUsername())));

        List<AdminOrderResponse> content = page.getContent().stream()
                .map(o -> {
                    List<OrderItemResponse> items = itemsByOrder.getOrDefault(o.getId(), List.of()).stream()
                            .map(OrderItemResponse::from).toList();
                    return AdminOrderResponse.of(o, usernameById.get(o.getUserId()), items);
                })
                .toList();

        return new AdminOrderPageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminSalesSummaryResponse salesSummary() {
        long gross = orderRepository.sumGrossRevenue();
        long refunded = orderRepository.sumRefundedAmount();
        return new AdminSalesSummaryResponse(
                gross,
                refunded,
                gross - refunded,
                orderRepository.countByStatus(OrderStatus.PAID),
                orderRepository.countByStatus(OrderStatus.REFUNDED),
                orderRepository.countByStatus(OrderStatus.PARTIAL_REFUNDED)
        );
    }

    @Transactional
    public OrderResponse forceRefund(Long adminId, Long orderId, List<Long> orderItemIds, RefundReason reason) {
        log.info("[Admin] 강제 환불 orderId={} items={} (by adminId={})", orderId, orderItemIds, adminId);
        return refundService.refundByAdmin(orderId, orderItemIds, reason);
    }
}
