package com.jes.devlearn.domain.course.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
