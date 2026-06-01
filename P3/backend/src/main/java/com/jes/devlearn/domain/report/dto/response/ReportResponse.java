package com.jes.devlearn.domain.report.dto.response;

import com.jes.devlearn.domain.report.entity.Report;
import com.jes.devlearn.domain.report.entity.ReportStatus;
import com.jes.devlearn.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterUsername,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        ReportStatus status,
        String resolverNote,
        LocalDateTime createdAt
) {
    public static ReportResponse of(Report report, String reporterUsername) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                reporterUsername,
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getResolverNote(),
                report.getCreatedAt()
        );
    }
}
