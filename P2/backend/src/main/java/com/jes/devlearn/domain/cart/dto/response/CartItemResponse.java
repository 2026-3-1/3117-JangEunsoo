package com.jes.devlearn.domain.cart.dto.response;

import com.jes.devlearn.domain.cart.entity.CartItem;
import com.jes.devlearn.domain.course.entity.Course;

import java.time.LocalDateTime;

public record CartItemResponse(
        Long id,
        Long courseId,
        String courseTitle,
        Long instructorId,
        String instructorName,
        Long price,
        LocalDateTime addedAt
) {
    public static CartItemResponse of(CartItem item, Course course) {
        return new CartItemResponse(
                item.getId(),
                course.getId(),
                course.getTitle(),
                course.getInstructorId(),
                course.getInstructorName(),
                course.getPrice(),
                item.getCreatedAt()
        );
    }
}
