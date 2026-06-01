package com.jes.devlearn.domain.admin.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AdminErrorCode implements ErrorCode {
    CANNOT_MODIFY_SELF(HttpStatus.BAD_REQUEST, "본인 계정의 역할/상태는 변경할 수 없습니다."),
    CANNOT_DEMOTE_ADMIN(HttpStatus.BAD_REQUEST, "관리자 계정의 역할은 변경할 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "이미 처리된 신고입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    AdminErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
