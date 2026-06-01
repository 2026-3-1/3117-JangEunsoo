package com.jes.devlearn.domain.admin.dto.request;

import com.jes.devlearn.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record AdminRoleUpdateRequest(
        @NotNull(message = "변경할 역할은 필수입니다.")
        Role role
) {
}
