package com.jes.devlearn.domain.report.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReportErrorCode implements ErrorCode {
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "이미 신고한 대상입니다."),
    CANNOT_REPORT_OWN(HttpStatus.BAD_REQUEST, "본인이 작성한 콘텐츠는 신고할 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ReportErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
