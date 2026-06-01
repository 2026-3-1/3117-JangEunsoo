package com.jes.devlearn.domain.admin.controller;

import com.jes.devlearn.domain.admin.dto.request.AdminResolveReportRequest;
import com.jes.devlearn.domain.admin.dto.response.AdminReportPageResponse;
import com.jes.devlearn.domain.admin.service.AdminReportService;
import com.jes.devlearn.domain.report.dto.response.ReportResponse;
import com.jes.devlearn.domain.report.entity.ReportStatus;
import com.jes.devlearn.domain.report.entity.ReportTargetType;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<AdminReportPageResponse>> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(adminReportService.list(status, targetType, pageable)));
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<GlobalApiResponse<ReportResponse>> resolve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reportId,
            @RequestBody(required = false) AdminResolveReportRequest req
    ) {
        boolean deleteTarget = req != null && req.deleteTarget();
        String note = req == null ? null : req.note();
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminReportService.resolve(principal.getUserId(), reportId, deleteTarget, note)));
    }

    @PostMapping("/{reportId}/dismiss")
    public ResponseEntity<GlobalApiResponse<ReportResponse>> dismiss(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reportId,
            @RequestBody(required = false) AdminResolveReportRequest req
    ) {
        String note = req == null ? null : req.note();
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminReportService.dismiss(principal.getUserId(), reportId, note)));
    }
}
