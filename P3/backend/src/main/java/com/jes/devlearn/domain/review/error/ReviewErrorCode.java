package com.jes.devlearn.domain.review.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "수강 중인 강의에만 리뷰를 작성할 수 있습니다."),
    REVIEW_PROGRESS_GATE(HttpStatus.UNPROCESSABLE_ENTITY, "리뷰는 진도율 80% 이상일 때 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
