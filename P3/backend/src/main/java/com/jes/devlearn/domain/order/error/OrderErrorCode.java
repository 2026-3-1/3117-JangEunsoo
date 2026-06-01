package com.jes.devlearn.domain.order.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "결제할 수 없는 주문 상태입니다."),
    ORDER_NOT_REFUNDABLE(HttpStatus.CONFLICT, "환불할 수 없는 주문 상태입니다."),
    INVALID_REFUND_ITEMS(HttpStatus.BAD_REQUEST, "환불 대상 항목이 올바르지 않습니다."),
    CART_SNAPSHOT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "장바구니 항목이 유효하지 않습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
