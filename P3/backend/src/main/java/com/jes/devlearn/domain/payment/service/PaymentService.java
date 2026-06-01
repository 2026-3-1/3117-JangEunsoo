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
}
