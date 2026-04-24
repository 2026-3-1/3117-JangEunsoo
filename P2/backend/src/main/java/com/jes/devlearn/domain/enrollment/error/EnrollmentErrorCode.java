package com.jes.devlearn.domain.enrollment.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EnrollmentErrorCode implements ErrorCode {

    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청 내역을 찾을 수 없습니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 수강 신청한 강의입니다."),
    COURSE_NOT_FREE(HttpStatus.BAD_REQUEST, "유료 강의는 결제를 통해서만 수강할 수 있습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
