package com.jes.devlearn.domain.qna.dto.response;

import com.jes.devlearn.domain.qna.entity.QnaAnswer;

import java.time.LocalDateTime;

public record QnaAnswerResponse(
        Long id,
        Long questionId,
        Long authorId,
        String authorUsername,
        String authorRole,
        String content,
        LocalDateTime createdAt
) {
    public static QnaAnswerResponse of(QnaAnswer answer, String authorUsername) {
        return new QnaAnswerResponse(
                answer.getId(),
                answer.getQuestionId(),
                answer.getAuthorId(),
                authorUsername,
                answer.getAuthorRole(),
                answer.getContent(),
                answer.getCreatedAt()
        );
    }
}
