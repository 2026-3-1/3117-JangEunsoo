package com.jes.devlearn.domain.enrollment.controller;

import com.jes.devlearn.domain.enrollment.dto.request.EnrollmentCreateRequestDTO;
import com.jes.devlearn.domain.enrollment.dto.response.EnrollmentResponseDTO;
import com.jes.devlearn.domain.enrollment.service.EnrollmentService;
import com.jes.devlearn.domain.progress.dto.response.ProgressRateResponseDTO;
import com.jes.devlearn.domain.progress.service.ProgressService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final ProgressService progressService;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<EnrollmentResponseDTO>> enroll(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EnrollmentCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.success(enrollmentService.enroll(principal.getUserId(), dto)));
    }

    @GetMapping("/me")
    public ResponseEntity<GlobalApiResponse<List<EnrollmentResponseDTO>>> getMyEnrollments(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(enrollmentService.getMyEnrollments(principal.getUserId())));
    }

    @GetMapping("/{id}/progress-rate")
    public ResponseEntity<GlobalApiResponse<ProgressRateResponseDTO>> getProgressRate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(progressService.getProgressRate(id, principal.getUserId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        enrollmentService.cancel(id, principal.getUserId());
        return ResponseEntity.ok(GlobalApiResponse.success("수강이 취소되었습니다."));
    }
}
