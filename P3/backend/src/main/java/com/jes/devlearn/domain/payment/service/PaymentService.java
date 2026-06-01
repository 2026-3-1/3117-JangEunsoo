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
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;

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
            Payment failed = paymentRepository.save(new Payment(
                    order.getId(), PaymentMethod.MOCK_CARD, PaymentStatus.FAILED, amount, null
            ));
            throw new CustomException(PaymentErrorCode.PAYMENT_FAILED);
        }

        Payment payment = paymentRepository.save(new Payment(
                order.getId(), PaymentMethod.MOCK_CARD, PaymentStatus.SUCCESS, amount, result.mockTransactionId()
        ));

        order.markPaid();

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        for (OrderItem item : items) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, item.getCourseId())) {
                enrollmentRepository.save(new Enrollment(userId, item.getCourseId()));
                notifyNewEnrollment(userId, item);
            }
        }

        cartItemRepository.deleteAllByUserId(userId);

        return PaymentResponse.from(payment);
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
