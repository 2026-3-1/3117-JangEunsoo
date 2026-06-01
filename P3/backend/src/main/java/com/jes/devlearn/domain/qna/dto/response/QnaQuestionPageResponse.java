package com.jes.devlearn.domain.qna.dto.response;

import java.util.List;

public record QnaQuestionPageResponse(
        List<QnaQuestionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
