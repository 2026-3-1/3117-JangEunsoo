package com.jes.devlearn.domain.report.dto.request;

import com.jes.devlearn.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상 유형은 필수입니다.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상 ID는 필수입니다.")
        Long targetId,

        @NotBlank(message = "신고 사유는 필수입니다.")
        @Size(max = 500)
        String reason
) {
}
