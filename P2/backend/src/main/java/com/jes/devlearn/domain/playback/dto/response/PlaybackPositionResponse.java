package com.jes.devlearn.domain.playback.dto.response;

import com.jes.devlearn.domain.playback.entity.PlaybackPosition;

import java.time.LocalDateTime;

public record PlaybackPositionResponse(
        Long enrollmentId,
        Long lectureId,
        Integer currentTimeSeconds,
        LocalDateTime updatedAt
) {
    public static PlaybackPositionResponse from(PlaybackPosition p) {
        return new PlaybackPositionResponse(
                p.getEnrollmentId(),
                p.getLectureId(),
                p.getCurrentTimeSeconds(),
                p.getUpdatedAt()
        );
    }
}
