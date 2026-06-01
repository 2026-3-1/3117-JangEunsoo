package com.jes.devlearn.domain.bookmark.dto.response;

import com.jes.devlearn.domain.bookmark.entity.Bookmark;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long id,
        Long lectureId,
        String lectureTitle,
        Long courseId,
        String courseTitle,
        Integer timeSeconds,
        String memo,
        LocalDateTime createdAt
) {
    public static BookmarkResponse of(Bookmark b, String lectureTitle, Long courseId, String courseTitle) {
        return new BookmarkResponse(
                b.getId(),
                b.getLectureId(),
                lectureTitle,
                courseId,
                courseTitle,
                b.getTimeSeconds(),
                b.getMemo(),
                b.getCreatedAt()
        );
    }
}
