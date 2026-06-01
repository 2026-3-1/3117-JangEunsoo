package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.instructor.dto.request.InstructorSectionRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorSectionResponse;
import com.jes.devlearn.domain.instructor.service.InstructorSectionService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instructor/courses/{courseId}/sections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorSectionController {

    private final InstructorSectionService instructorSectionService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<InstructorSectionResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorSectionService.list(principal.getUserId(), courseId)));
    }

    @PostMapping
    public ResponseEntity<GlobalApiResponse<InstructorSectionResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody InstructorSectionRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorSectionService.create(principal.getUserId(), courseId, req)));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<GlobalApiResponse<InstructorSectionResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody InstructorSectionRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorSectionService.update(principal.getUserId(), courseId, sectionId, req)));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId
    ) {
        instructorSectionService.delete(principal.getUserId(), courseId, sectionId);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
