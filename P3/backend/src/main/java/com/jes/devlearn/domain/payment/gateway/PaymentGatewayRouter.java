package com.jes.devlearn.domain.payment.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 결제 게이트웨이 선택기. TOSS_SECRET_KEY가 설정돼 있으면 Toss, 아니면 Mock으로 위임.
 * (키 미설정 환경 — 개발·CI·시연 — 에서도 결제 플로우가 끊기지 않도록 보장)
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final TossPaymentGateway tossPaymentGateway;
    private final MockPaymentGateway mockPaymentGateway;

    public PaymentGateway active() {
        return tossPaymentGateway.isAvailable() ? tossPaymentGateway : mockPaymentGateway;
    }
}
