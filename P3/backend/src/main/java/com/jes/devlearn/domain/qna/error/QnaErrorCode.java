package com.jes.devlearn.domain.qna.error;

import com.jes.devlearn.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum QnaErrorCode implements ErrorCode {
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
    NOT_ENROLLED(HttpStatus.FORBIDDEN, "해당 강의 수강생만 질문을 작성할 수 있습니다."),
    NOT_ANSWERABLE(HttpStatus.FORBIDDEN, "강사 또는 관리자만 답변할 수 있습니다."),
    QNA_ACCESS_DENIED(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    NOT_QUESTION_OWNER(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    NOT_ANSWER_OWNER(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    QnaErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
