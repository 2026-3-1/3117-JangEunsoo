package com.jes.devlearn.domain.report.controller;

import com.jes.devlearn.domain.report.dto.request.ReportCreateRequest;
import com.jes.devlearn.domain.report.dto.response.ReportResponse;
import com.jes.devlearn.domain.report.service.ReportService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<ReportResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportCreateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(reportService.create(principal.getUserId(), req)));
    }
}
