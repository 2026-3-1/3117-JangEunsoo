package com.jes.devlearn.domain.admin.controller;

import com.jes.devlearn.domain.admin.dto.request.AdminRoleUpdateRequest;
import com.jes.devlearn.domain.admin.dto.response.AdminUserPageResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminUserResponse;
import com.jes.devlearn.domain.admin.service.AdminUserService;
import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<AdminUserPageResponse>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(adminUserService.list(role, keyword, pageable)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalApiResponse<AdminUserResponse>> get(@PathVariable Long userId) {
        return ResponseEntity.ok(GlobalApiResponse.success(adminUserService.get(userId)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<GlobalApiResponse<AdminUserResponse>> changeRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminRoleUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminUserService.changeRole(principal.getUserId(), userId, req.role())));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<GlobalApiResponse<AdminUserResponse>> deactivate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminUserService.setActive(principal.getUserId(), userId, false)));
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<GlobalApiResponse<AdminUserResponse>> activate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminUserService.setActive(principal.getUserId(), userId, true)));
    }
}
