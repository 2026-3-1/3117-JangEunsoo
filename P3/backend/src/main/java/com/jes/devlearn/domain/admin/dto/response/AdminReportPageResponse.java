package com.jes.devlearn.domain.admin.dto.response;

import com.jes.devlearn.domain.report.dto.response.ReportResponse;

import java.util.List;

public record AdminReportPageResponse(
        List<ReportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
