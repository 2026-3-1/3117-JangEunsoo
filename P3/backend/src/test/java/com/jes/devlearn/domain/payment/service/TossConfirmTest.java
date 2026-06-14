package com.jes.devlearn.domain.payment.service;

import com.jes.devlearn.domain.cart.repository.CartItemRepository;
import com.jes.devlearn.domain.enrollment.entity.Enrollment;
import com.jes.devlearn.domain.enrollment.repository.EnrollmentRepository;
import com.jes.devlearn.domain.order.entity.Order;
import com.jes.devlearn.domain.order.entity.OrderItem;
import com.jes.devlearn.domain.order.error.OrderErrorCode;
import com.jes.devlearn.domain.order.repository.OrderItemRepository;
import com.jes.devlearn.domain.order.repository.OrderRepository;
import com.jes.devlearn.domain.payment.error.PaymentErrorCode;
import com.jes.devlearn.domain.payment.gateway.MockPaymentGateway;
import com.jes.devlearn.domain.payment.gateway.PaymentGatewayRouter;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService.confirm (Toss/Mock 승인 경로)")
class TossConfirmTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private MockPaymentGateway mockPaymentGateway;
    @Mock private PaymentGatewayRouter paymentGatewayRouter;
    @Mock private com.jes.devlearn.domain.course.repository.CourseRepository courseRepository;
    @Mock private com.jes.devlearn.domain.notification.service.NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("클라가 위조한 금액 ≠ 서버 재계산 금액 → 422 PAYMENT_AMOUNT_MISMATCH")
    void amount_mismatch_returns_422() throws Exception {
        Order order = new Order("ORD-20260614-0001", 5L, 30000L);
        setId(order, 1L);
        when(orderRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1L))
                .thenReturn(List.of(new OrderItem(1L, 100L, "강의1", 30000L)));

        // 클라가 1원으로 위조 → 서버 재계산 30000원과 불일치
        assertThatThrownBy(() -> paymentService.confirm(5L, 1L, "pk_test_abc", 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("타인 주문 confirm → 404 ORDER_NOT_FOUND")
    void other_user_order_returns_404() {
        when(orderRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirm(99L, 1L, "pk_test_abc", 30000L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("금액 일치 + Mock fallback 승인 성공 → enrollment 생성·cart 비움")
    void confirm_success_creates_enrollment_and_clears_cart() throws Exception {
        Order order = new Order("ORD-20260614-0002", 5L, 30000L);
        setId(order, 1L);
        when(orderRepository.findByIdAndUserId(1L, 5L)).thenReturn(Optional.of(order));

        OrderItem item = new OrderItem(1L, 100L, "강의1", 30000L);
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(item));

        // TOSS 키 미설정 환경 가정 → 라우터가 Mock 게이트웨이 반환
        when(paymentGatewayRouter.active()).thenReturn(mockPaymentGateway);
        when(mockPaymentGateway.confirm(order.getOrderNo(), "pk_test_abc", 30000L))
                .thenReturn(new com.jes.devlearn.domain.payment.gateway.PaymentGateway.ConfirmResult(true, "MOCK-TX", "OK"));
        lenient().when(mockPaymentGateway.method())
                .thenReturn(com.jes.devlearn.domain.payment.entity.PaymentMethod.MOCK_CARD);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.existsByUserIdAndCourseId(5L, 100L)).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.confirm(5L, 1L, "pk_test_abc", 30000L);

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(cartItemRepository).deleteAllByUserId(5L);
    }

    private void setId(Order order, Long id) throws Exception {
        Field f = Order.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(order, id);
    }
}
