package com.jes.devlearn.domain.enrollment.dto.response;

import com.jes.devlearn.domain.enrollment.entity.Enrollment;

import java.time.LocalDateTime;

public record EnrollmentResponseDTO(
        Long id,
        Long courseId,
        LocalDateTime createdAt
) {
    public static EnrollmentResponseDTO from(Enrollment enrollment) {
        return new EnrollmentResponseDTO(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getCreatedAt()
        );
    }
}
