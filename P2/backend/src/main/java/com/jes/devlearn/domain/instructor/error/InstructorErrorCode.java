package com.jes.devlearn.domain.instructor.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InstructorErrorCode implements ErrorCode {

    NOT_INSTRUCTOR(HttpStatus.FORBIDDEN, "강사 권한이 필요합니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "강사 프로필이 없습니다."),
    PUBLISH_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "발행 조건을 만족하지 않습니다. 섹션과 렉처를 최소 1개 이상 추가하세요."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않은 상태 전환입니다."),
    ALREADY_PUBLISHED(HttpStatus.CONFLICT, "이미 발행된 강의입니다."),
    ALREADY_ARCHIVED(HttpStatus.CONFLICT, "이미 보관된 강의입니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다."),
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "렉처를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
