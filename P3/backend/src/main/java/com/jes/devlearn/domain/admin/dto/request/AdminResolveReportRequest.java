package com.jes.devlearn.domain.admin.dto.request;

import jakarta.validation.constraints.Size;

public record AdminResolveReportRequest(
        // true면 신고 대상 콘텐츠를 삭제하고 RESOLVED, false면 대상 유지하며 RESOLVED
        boolean deleteTarget,

        @Size(max = 500)
        String note
) {
}
