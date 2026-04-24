package com.jes.devlearn.domain.payment.gateway;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGateway {

    public record Result(boolean success, String mockTransactionId, String message) {}

    public Result process(Long orderId, Long amount, boolean simulateFailure) {
        if (simulateFailure) {
            return new Result(false, null, "Simulated failure");
        }
        String txId = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new Result(true, txId, "OK");
    }
}
