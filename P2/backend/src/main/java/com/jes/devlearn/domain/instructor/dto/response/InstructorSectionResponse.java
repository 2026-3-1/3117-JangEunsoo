package com.jes.devlearn.domain.instructor.dto.response;

import com.jes.devlearn.domain.course.entity.Section;

public record InstructorSectionResponse(
        Long id,
        Long courseId,
        String title,
        Integer orderNum
) {
    public static InstructorSectionResponse from(Section section) {
        return new InstructorSectionResponse(
                section.getId(),
                section.getCourseId(),
                section.getTitle(),
                section.getOrderNum()
        );
    }
}
