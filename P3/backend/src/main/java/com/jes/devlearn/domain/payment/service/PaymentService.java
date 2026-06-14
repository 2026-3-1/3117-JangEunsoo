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
import com.jes.devlearn.domain.payment.dto.response.PaymentResponse;
import com.jes.devlearn.domain.payment.entity.Payment;
import com.jes.devlearn.domain.payment.entity.PaymentMethod;
import com.jes.devlearn.domain.payment.entity.PaymentStatus;
import com.jes.devlearn.domain.payment.error.PaymentErrorCode;
import com.jes.devlearn.domain.payment.gateway.MockPaymentGateway;
import com.jes.devlearn.domain.payment.gateway.PaymentGateway;
import com.jes.devlearn.domain.payment.gateway.PaymentGatewayRouter;
import com.jes.devlearn.domain.payment.repository.PaymentRepository;
import com.jes.devlearn.domain.course.entity.Course;
import com.jes.devlearn.domain.course.repository.CourseRepository;
import com.jes.devlearn.domain.notification.service.NotificationService;
import com.jes.devlearn.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartItemRepository cartItemRepository;
    private final MockPaymentGateway mockPaymentGateway;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;

    /**
     * 기존 모의 결제 경로(하위호환). 프론트가 모의 결제 버튼을 쓸 때 사용.
     * 금액은 서버 재계산만 신뢰.
     */
    @Transactional
    public PaymentResponse checkout(Long userId, Long orderId, boolean simulateFailure) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_PAYABLE);
        }

        long amount = recalcAmount(order);

        MockPaymentGateway.Result result = mockPaymentGateway.process(order.getId(), amount, simulateFailure);
        if (!result.success()) {
            paymentRepository.save(new Payment(
                    order.getId(), PaymentMethod.MOCK_CARD, PaymentStatus.FAILED, amount, null
            ));
            throw new CustomException(PaymentErrorCode.PAYMENT_FAILED);
        }

        Payment payment = paymentRepository.save(new Payment(
                order.getId(), PaymentMethod.MOCK_CARD, PaymentStatus.SUCCESS, amount, result.mockTransactionId()
        ));

        finalizePaidOrder(userId, order);
        return PaymentResponse.from(payment);
    }

    /**
     * Toss(또는 키 미설정 시 Mock fallback) 결제 승인 경로.
     * 1) 주문 소유·PENDING 검증 → 2) 금액 서버 재계산 → 3) 클라가 보낸 amount와 대조(위변조 차단)
     * → 4) 게이트웨이 confirm → 5) enrollment 생성·장바구니 비움·알림.
     */
    @Transactional
    public PaymentResponse confirm(Long userId, Long orderId, String paymentKey, long clientAmount) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_PAYABLE);
        }

        long amount = recalcAmount(order);
        // 클라이언트가 전달한 금액은 신뢰하지 않고 서버 재계산 값과 일치하는지만 확인
        if (amount != clientAmount) {
            throw new CustomException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentGateway gateway = paymentGatewayRouter.active();
        PaymentGateway.ConfirmResult result = gateway.confirm(order.getOrderNo(), paymentKey, amount);
        if (!result.success()) {
            paymentRepository.save(new Payment(
                    order.getId(), gateway.method(), PaymentStatus.FAILED, amount, null
            ));
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        Payment payment = paymentRepository.save(new Payment(
                order.getId(), gateway.method(), PaymentStatus.SUCCESS, amount, result.transactionId()
        ));

        finalizePaidOrder(userId, order);
        return PaymentResponse.from(payment);
    }

    /** 결제 성공 공통 후처리: 주문 PAID 전이 + enrollment 멱등 생성 + 장바구니 비움. */
    private void finalizePaidOrder(Long userId, Order order) {
        order.markPaid();

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        for (OrderItem item : items) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, item.getCourseId())) {
                enrollmentRepository.save(new Enrollment(userId, item.getCourseId()));
                notifyNewEnrollment(userId, item);
            }
        }

        cartItemRepository.deleteAllByUserId(userId);
    }

    private long recalcAmount(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId()).stream()
                .filter(OrderItem::isActive)
                .mapToLong(OrderItem::getPriceSnapshot)
                .sum();
    }

    private void notifyNewEnrollment(Long userId, OrderItem item) {
        // dedupKey: 사용자×강의 단위 — 동일 결제 재처리 시 중복 알림 방지
        String dedupKey = "enroll:" + userId + ":" + item.getCourseId();
        Long instructorId = courseRepository.findById(item.getCourseId())
                .map(Course::getInstructorId).orElse(null);
        String message = String.format("'%s' 강의에 새 수강생(사용자 #%d)이 결제로 등록되었습니다.%s",
                item.getCourseTitleSnapshot(), userId,
                instructorId == null ? "" : " (강사 #" + instructorId + ")");
        notificationService.enqueue(dedupKey, "NEW_ENROLLMENT", "신규 수강·결제", message);
    }
}
