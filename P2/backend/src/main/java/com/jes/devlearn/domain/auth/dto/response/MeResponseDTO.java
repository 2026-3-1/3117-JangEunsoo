package com.jes.devlearn.domain.auth.dto.response;

import com.jes.devlearn.domain.user.entity.Role;

public record MeResponseDTO(
        Long userId,
        String username,
        Role role
) {
}
