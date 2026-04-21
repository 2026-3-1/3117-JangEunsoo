package com.jes.devlearn.domain.review.dto.response;

import com.jes.devlearn.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponseDTO(
        Long id,
        Long userId,
        Long courseId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponseDTO from(Review review) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getUserId(),
                review.getCourseId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
