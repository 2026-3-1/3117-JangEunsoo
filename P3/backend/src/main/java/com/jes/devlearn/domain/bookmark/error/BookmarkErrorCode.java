package com.jes.devlearn.domain.bookmark.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements ErrorCode {

    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "해당 강의에 수강 중이 아닙니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
