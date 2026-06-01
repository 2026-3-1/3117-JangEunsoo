package com.jes.devlearn.domain.cart.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {

    CART_DUPLICATE(HttpStatus.CONFLICT, "이미 장바구니에 담긴 강의입니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다."),
    COURSE_NOT_PURCHASABLE(HttpStatus.CONFLICT, "구매할 수 없는 강의입니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 수강 중인 강의입니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
