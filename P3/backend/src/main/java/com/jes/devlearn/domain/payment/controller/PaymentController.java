package com.jes.devlearn.domain.payment.controller;

import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.payment.dto.request.CheckoutRequest;
import com.jes.devlearn.domain.payment.dto.request.RefundRequest;
import com.jes.devlearn.domain.payment.dto.request.TossConfirmRequest;
import com.jes.devlearn.domain.payment.dto.response.PaymentResponse;
import com.jes.devlearn.domain.payment.entity.RefundReason;
import com.jes.devlearn.domain.payment.service.PaymentService;
import com.jes.devlearn.domain.payment.service.RefundService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/checkout")
    public ResponseEntity<GlobalApiResponse<PaymentResponse>> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest req,
            @RequestParam(defaultValue = "false") boolean simulateFailure
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                paymentService.checkout(principal.getUserId(), req.orderId(), simulateFailure)
        ));
    }

    /**
     * Toss 결제 성공 콜백 후 프론트가 호출하는 승인 엔드포인트.
     * 금액은 서버에서 재계산해 검증(클라 amount는 대조용). TOSS_SECRET_KEY 미설정 시 Mock으로 승인.
     */
    @PostMapping("/confirm")
    public ResponseEntity<GlobalApiResponse<PaymentResponse>> confirm(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TossConfirmRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                paymentService.confirm(principal.getUserId(), req.orderId(), req.paymentKey(), req.amount())
        ));
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<GlobalApiResponse<OrderResponse>> refund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest req
    ) {
        RefundReason reason = (req == null || req.reason() == null) ? RefundReason.USER_REQUEST : req.reason();
        return ResponseEntity.ok(GlobalApiResponse.success(
                refundService.refund(principal.getUserId(), orderId, req == null ? null : req.orderItemIds(), reason)
        ));
    }
}
