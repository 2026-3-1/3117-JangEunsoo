package com.jes.devlearn.domain.progress.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProgressErrorCode implements ErrorCode {

    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의 항목을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
