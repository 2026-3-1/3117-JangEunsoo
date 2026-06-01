package com.jes.devlearn.domain.playback.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaybackErrorCode implements ErrorCode {

    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 정보를 찾을 수 없습니다."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "해당 강의에 수강 중이 아닙니다."),
    LECTURE_NOT_IN_COURSE(HttpStatus.BAD_REQUEST, "해당 렉처가 강의에 속해있지 않습니다."),
    PLAYBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "재생 위치를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
