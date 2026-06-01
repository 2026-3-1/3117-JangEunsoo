package com.jes.devlearn.domain.qna.dto.response;

import com.jes.devlearn.domain.qna.entity.QnaQuestion;

import java.time.LocalDateTime;
import java.util.List;

public record QnaQuestionResponse(
        Long id,
        Long courseId,
        Long authorId,
        String authorUsername,
        String title,
        String content,
        boolean isPrivate,
        boolean answered,
        int answerCount,
        LocalDateTime createdAt,
        List<QnaAnswerResponse> answers
) {
    public static QnaQuestionResponse summary(QnaQuestion q, String authorUsername) {
        return new QnaQuestionResponse(
                q.getId(), q.getCourseId(), q.getAuthorId(), authorUsername,
                q.getTitle(), q.getContent(), q.isPrivate(), q.isAnswered(),
                q.getAnswerCount(), q.getCreatedAt(), null
        );
    }

    public static QnaQuestionResponse detail(QnaQuestion q, String authorUsername, List<QnaAnswerResponse> answers) {
        return new QnaQuestionResponse(
                q.getId(), q.getCourseId(), q.getAuthorId(), authorUsername,
                q.getTitle(), q.getContent(), q.isPrivate(), q.isAnswered(),
                q.getAnswerCount(), q.getCreatedAt(), answers
        );
    }
}
