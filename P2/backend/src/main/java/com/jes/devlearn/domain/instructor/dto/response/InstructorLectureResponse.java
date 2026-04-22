package com.jes.devlearn.domain.instructor.dto.response;

import com.jes.devlearn.domain.course.entity.Lecture;

public record InstructorLectureResponse(
        Long id,
        Long sectionId,
        String title,
        String videoUrl,
        Integer orderNum,
        Integer durationSeconds
) {
    public static InstructorLectureResponse from(Lecture lecture) {
        return new InstructorLectureResponse(
                lecture.getId(),
                lecture.getSectionId(),
                lecture.getTitle(),
                lecture.getVideoUrl(),
                lecture.getOrderNum(),
                lecture.getDurationSeconds()
        );
    }
}
