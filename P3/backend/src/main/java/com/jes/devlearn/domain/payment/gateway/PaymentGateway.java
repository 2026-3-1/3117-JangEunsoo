package com.jes.devlearn.domain.payment.gateway;

import com.jes.devlearn.domain.payment.entity.PaymentMethod;

/**
 * 결제 승인(confirm) 게이트웨이 추상화.
 * 실 연동(Toss)과 모의(Mock) 구현을 동일 인터페이스로 다룬다.
 */
public interface PaymentGateway {

    /** 결제 승인 결과. transactionId는 PG 거래키(Toss paymentKey 등). */
    record ConfirmResult(boolean success, String transactionId, String message) {}

    /**
     * @param orderNo    주문번호(가맹점 주문 식별자)
     * @param paymentKey PG가 발급한 결제 키(Mock은 null/임의)
     * @param amount     서버가 재계산해 검증한 결제 금액(원)
     */
    ConfirmResult confirm(String orderNo, String paymentKey, long amount);

    /** 이 게이트웨이로 승인된 결제의 method 표기. */
    PaymentMethod method();

    /** 활성 여부(키 미설정 시 false → 다른 게이트웨이로 fallback). */
    boolean isAvailable();
}
