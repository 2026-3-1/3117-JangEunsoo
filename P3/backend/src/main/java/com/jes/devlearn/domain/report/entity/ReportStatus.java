package com.jes.devlearn.domain.report.entity;

public enum ReportStatus {
    PENDING,    // 접수, 미처리
    RESOLVED,   // 처리 완료 (대상 삭제 등 조치)
    DISMISSED   // 반려 (조치 불필요)
}
