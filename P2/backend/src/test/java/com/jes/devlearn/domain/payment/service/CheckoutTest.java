package com.jes.devlearn.domain.payment.service;

import com.jes.devlearn.domain.cart.repository.CartItemRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.error.OrderErrorCode;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.domain.payment.gateway.MockPaymentGateway;
import com.jes.devlearn.domain.payment.repository.PaymentRepository;
import com.jes.devlearn.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService.checkout 상태머신")
class CheckoutTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private MockPaymentGateway mockPaymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("타인의 주문 결제 시도 → 404 ORDER_NOT_FOUND")
    void other_user_order_returns_404() {
        when(orderRepository.findByIdAndUserId(eq(1L), eq(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.checkout(99L, 1L, false))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 PAID 상태인 주문에 checkout → 409 ORDER_NOT_PAYABLE")
    void paid_order_again_returns_409() throws Exception {
        Order order = new Order("ORD-20260424-0001", 5L, 50000L);
        setOrderStatus(order, OrderStatus.PAID);
        setId(order, 1L);
        when(orderRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.checkout(5L, 1L, false))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(OrderErrorCode.ORDER_NOT_PAYABLE);
    }

    @Test
    @DisplayName("PENDING 주문 결제 성공 → enrollment 자동 생성, cart 비움")
    void pending_order_success_creates_enrollments_and_clears_cart() throws Exception {
        Order order = new Order("ORD-20260424-0001", 5L, 30000L);
        setId(order, 1L);
        when(orderRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.of(order));

        OrderItem item1 = new OrderItem(1L, 100L, "강의1", 30000L);
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(item1));

        when(mockPaymentGateway.process(anyLong(), eq(30000L), eq(false)))
                .thenReturn(new MockPaymentGateway.Result(true, "MOCK-XYZ", "OK"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 100L)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.checkout(5L, 1L, false);

        org.mockito.Mockito.verify(enrollmentRepository).save(any(Enrollment.class));
        org.mockito.Mockito.verify(cartItemRepository).deleteAllByUserId(5L);
    }

    private void setId(Order order, Long id) throws Exception {
        Field f = Order.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(order, id);
    }

    private void setOrderStatus(Order order, OrderStatus status) throws Exception {
        Field f = Order.class.getDeclaredField("status");
        f.setAccessible(true);
        f.set(order, status);
    }
}
