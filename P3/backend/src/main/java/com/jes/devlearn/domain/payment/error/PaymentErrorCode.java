package com.jes.devlearn.domain.payment.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제에 실패했습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제 승인에 실패했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
