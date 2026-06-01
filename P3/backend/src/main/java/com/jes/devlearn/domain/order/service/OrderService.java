package com.jes.devlearn.domain.order.service;

import com.jes.devlearn.domain.cart.entity.CartItem;
import com.jes.devlearn.domain.cart.error.CartErrorCode;
import com.jes.devlearn.domain.cart.repository.CartItemRepository;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.course.error.CourseErrorCode;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.order.dto.response.OrderItemResponse;
import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.error.OrderErrorCode;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public OrderResponse createFromCart(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new CustomException(CartErrorCode.EMPTY_CART);
        }
        List<Long> courseIds = cartItems.stream().map(CartItem::getCourseId).toList();
        Map<Long, Course> courseMap = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(c -> courseMap.put(c.getId(), c));

        long total = 0L;
        for (CartItem ci : cartItems) {
            Course course = courseMap.get(ci.getCourseId());
            if (course == null) {
                throw new CustomException(OrderErrorCode.CART_SNAPSHOT_INVALID);
            }
            if (course.getPublishStatus() != PublishStatus.PUBLISHED) {
                throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
            }
            if (course.isFree()) {
                throw new CustomException(OrderErrorCode.CART_SNAPSHOT_INVALID);
            }
            if (enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new CustomException(CartErrorCode.ALREADY_ENROLLED);
            }
            total += course.getPrice();
        }

        String orderNo = nextOrderNo();
        Order order = orderRepository.save(new Order(orderNo, userId, total));

        for (CartItem ci : cartItems) {
            Course course = courseMap.get(ci.getCourseId());
            orderItemRepository.save(new OrderItem(
                    order.getId(),
                    course.getId(),
                    course.getTitle(),
                    course.getPrice()
            ));
        }
        return loadOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMine(Long userId, OrderStatus status) {
        List<Order> orders = (status == null)
                ? orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                : orderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        if (orders.isEmpty()) return List.of();
        List<Long> ids = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        for (OrderItem item : orderItemRepository.findAllByOrderIdIn(ids)) {
            itemsByOrder.computeIfAbsent(item.getOrderId(), k -> new java.util.ArrayList<>()).add(item);
        }
        return orders.stream()
                .map(o -> {
                    List<OrderItemResponse> itemDtos = itemsByOrder.getOrDefault(o.getId(), List.of()).stream()
                            .map(OrderItemResponse::from).toList();
                    return OrderResponse.of(o, itemDtos);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        return loadOrderResponse(order);
    }

    private OrderResponse loadOrderResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findAllByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from).toList();
        return OrderResponse.of(order, items);
    }

    private String nextOrderNo() {
        String prefix = "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long count = orderRepository.countByOrderNoStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }
}
