package com.jes.devlearn.domain.admin.controller;

import com.jes.devlearn.domain.admin.dto.request.AdminBlockCourseRequest;
import com.jes.devlearn.domain.admin.dto.response.AdminCoursePageResponse;
import com.jes.devlearn.domain.admin.dto.response.AdminCourseResponse;
import com.jes.devlearn.domain.admin.service.AdminCourseService;
import com.jes.devlearn.domain.course.entity.PublishStatus;
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
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<AdminCoursePageResponse>> list(
            @RequestParam(required = false) PublishStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(adminCourseService.list(status, keyword, pageable)));
    }

    @PostMapping("/{courseId}/block")
    public ResponseEntity<GlobalApiResponse<AdminCourseResponse>> block(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody AdminBlockCourseRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminCourseService.block(principal.getUserId(), courseId, req.reason())));
    }

    @PostMapping("/{courseId}/unblock")
    public ResponseEntity<GlobalApiResponse<AdminCourseResponse>> unblock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminCourseService.unblock(principal.getUserId(), courseId)));
    }
}
