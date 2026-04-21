package com.jes.devlearn.domain.course.dto.response;

import com.jes.devlearn.domain.course.entity.Course;
import org.springframework.data.domain.Page;

import java.util.List;

public record CoursePageResponseDTO(
        List<CourseResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CoursePageResponseDTO from(Page<Course> page) {
        return new CoursePageResponseDTO(
                page.getContent().stream().map(CourseResponseDTO::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}