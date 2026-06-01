package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Section;

import java.util.List;

public record SectionResponseDTO(
        Long id,
        String title,
        Integer orderNum,
        List<LectureResponseDTO> lectures
) {
    public static SectionResponseDTO from(Section section, List<LectureResponseDTO> lectures) {
        return new SectionResponseDTO(
                section.getId(),
                section.getTitle(),
                section.getOrderNum(),
                lectures
        );
    }
}
