package com.jes.devlearn.domain.payment.gateway;

import com.jes.devlearn.domain.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 모의 결제 게이트웨이. 실 PG 키 미설정 환경(개발·CI·시연)에서 항상 성공 처리.
 * PaymentGateway 구현(신규 confirm 경로) + 기존 process(...) 경로(하위호환) 모두 제공.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    public record Result(boolean success, String mockTransactionId, String message) {}

    /** 기존 checkout 경로(하위호환). simulateFailure로 실패 시뮬레이션. */
    public Result process(Long orderId, Long amount, boolean simulateFailure) {
        if (simulateFailure) {
            return new Result(false, null, "Simulated failure");
        }
        return new Result(true, newTxId(), "OK");
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.MOCK_CARD;
    }

    @Override
    public boolean isAvailable() {
        return true; // 항상 가능 — 최종 fallback
    }

    @Override
    public ConfirmResult confirm(String orderNo, String paymentKey, long amount) {
        return new ConfirmResult(true, newTxId(), "OK");
    }

    private String newTxId() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
