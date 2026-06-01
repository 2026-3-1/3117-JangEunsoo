package com.jes.devlearn.domain.admin.dto.response;

import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
