package com.jes.devlearn.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBlockCourseRequest(
        @NotBlank(message = "차단 사유는 필수입니다.")
        @Size(max = 500)
        String reason
) {
}
