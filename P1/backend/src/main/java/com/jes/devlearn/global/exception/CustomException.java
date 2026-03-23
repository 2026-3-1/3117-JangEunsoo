package com.jes.devlearn.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String defaultMessage){
        super(defaultMessage);
        this.errorCode = errorCode;
    }

    public int getStatus() {
        return errorCode.getStatus().value();
    }
}
