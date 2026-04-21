package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Lecture;

public record LectureResponseDTO(
        Long id,
        String title,
        String videoUrl,
        Integer orderNum
) {
    public static LectureResponseDTO from(Lecture lecture) {
        return new LectureResponseDTO(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getVideoUrl(),
                lecture.getOrderNum()
        );
    }
}
