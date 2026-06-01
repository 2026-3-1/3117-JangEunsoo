package com.jes.devlearn.domain.admin.dto.response;

import java.util.List;

public record AdminCoursePageResponse(
        List<AdminCourseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
