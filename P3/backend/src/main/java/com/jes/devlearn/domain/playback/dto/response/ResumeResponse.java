package com.jes.devlearn.domain.playback.dto.response;

public record ResumeResponse(
        Long enrollmentId,
        Long courseId,
        Long lectureId,
        Integer currentTimeSeconds
) {
}
