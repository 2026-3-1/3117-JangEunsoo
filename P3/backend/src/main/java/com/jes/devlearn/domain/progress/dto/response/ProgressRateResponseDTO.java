package com.jes.devlearn.domain.progress.dto.response;

public record ProgressRateResponseDTO(
        Long enrollmentId,
        Long courseId,
        int totalLectures,
        int completedLectures,
        double progressRate
) {}
