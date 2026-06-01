package com.jes.devlearn.domain.category.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
