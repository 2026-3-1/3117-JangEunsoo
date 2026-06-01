package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.instructor.dto.request.InstructorProfileUpdateRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorProfileResponse;
import com.jes.devlearn.domain.instructor.service.InstructorProfileService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructor/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorProfileController {

    private final InstructorProfileService instructorProfileService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<InstructorProfileResponse>> getMine(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorProfileService.getMine(principal.getUserId())));
    }

    @PutMapping
    public ResponseEntity<GlobalApiResponse<InstructorProfileResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InstructorProfileUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorProfileService.upsertMine(principal.getUserId(), req)));
    }
}
