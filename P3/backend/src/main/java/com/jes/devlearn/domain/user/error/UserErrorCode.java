package com.jes.devlearn.domain.user.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "Username already exists");

    private final HttpStatus status;
    private final String defaultMessage;

    UserErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
