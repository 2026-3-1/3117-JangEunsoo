package com.jes.devlearn.domain.payment.gateway;

import com.jes.devlearn.domain.payment.entity.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Toss Payments 결제 승인 연동.
 * TOSS_SECRET_KEY 미설정 시 isAvailable()=false → PaymentGatewayRouter가 Mock으로 fallback.
 * 인증: Basic base64(secretKey + ":")  (Toss 규약 — 시크릿키를 username, 비밀번호는 공란)
 */
@Slf4j
@Component
public class TossPaymentGateway implements PaymentGateway {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final String secretKey;
    private final RestClient restClient;

    public TossPaymentGateway(@Value("${app.payment.toss.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.TOSS;
    }

    @Override
    public boolean isAvailable() {
        return secretKey != null && !secretKey.isBlank();
    }

    @Override
    public ConfirmResult confirm(String orderNo, String paymentKey, long amount) {
        if (!isAvailable()) {
            return new ConfirmResult(false, null, "Toss secret key 미설정");
        }
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        try {
            Map<?, ?> body = restClient.post()
                    .uri(CONFIRM_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderNo,
                            "amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);

            String txId = body != null && body.get("paymentKey") != null
                    ? String.valueOf(body.get("paymentKey"))
                    : paymentKey;
            log.info("[Toss] 결제 승인 성공 orderNo={} amount={}", orderNo, amount);
            return new ConfirmResult(true, txId, "OK");
        } catch (Exception e) {
            // 4xx(금액 불일치·중복 승인 등)·5xx·네트워크 오류
            log.warn("[Toss] 결제 승인 실패 orderNo={} : {}", orderNo, e.getMessage());
            return new ConfirmResult(false, null, e.getMessage());
        }
    }
}
