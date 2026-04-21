package com.jes.devlearn.domain.auth.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "token invalid"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token expired"),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "token not found");

    private final HttpStatus status;
    private final String defaultMessage;

    AuthErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
