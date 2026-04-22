package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.instructor.dto.request.InstructorLectureRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorLectureResponse;
import com.jes.devlearn.domain.instructor.service.InstructorLectureService;
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
@RequestMapping("/api/instructor/courses/{courseId}/sections/{sectionId}/lectures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorLectureController {

    private final InstructorLectureService instructorLectureService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<InstructorLectureResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorLectureService.list(principal.getUserId(), courseId, sectionId)));
    }

    @PostMapping
    public ResponseEntity<GlobalApiResponse<InstructorLectureResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody InstructorLectureRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorLectureService.create(principal.getUserId(), courseId, sectionId, req)));
    }

    @PutMapping("/{lectureId}")
    public ResponseEntity<GlobalApiResponse<InstructorLectureResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lectureId,
            @Valid @RequestBody InstructorLectureRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorLectureService.update(principal.getUserId(), courseId, sectionId, lectureId, req)));
    }

    @DeleteMapping("/{lectureId}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lectureId
    ) {
        instructorLectureService.delete(principal.getUserId(), courseId, sectionId, lectureId);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
