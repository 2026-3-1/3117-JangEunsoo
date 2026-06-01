package com.jes.devlearn.domain.admin.dto.response;

import com.jes.devlearn.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminUserPageResponse from(Page<User> page) {
        return new AdminUserPageResponse(
                page.getContent().stream().map(AdminUserResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
