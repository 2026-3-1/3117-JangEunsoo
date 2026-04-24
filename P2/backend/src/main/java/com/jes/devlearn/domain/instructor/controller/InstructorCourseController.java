package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.course.entity.PublishStatus;
import com.jes.devlearn.domain.instructor.dto.request.InstructorCourseCreateRequest;
import com.jes.devlearn.domain.instructor.dto.request.InstructorCourseUpdateRequest;
import com.jes.devlearn.domain.instructor.dto.response.InstructorCourseResponse;
import com.jes.devlearn.domain.instructor.service.InstructorCourseCancellationService;
import com.jes.devlearn.domain.instructor.service.InstructorCourseService;
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
@RequestMapping("/api/instructor/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorCourseController {

    private final InstructorCourseService instructorCourseService;
    private final InstructorCourseCancellationService instructorCourseCancellationService;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<InstructorCourseResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InstructorCourseCreateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorCourseService.create(principal.getUserId(), req)));
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<InstructorCourseResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) PublishStatus status
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorCourseService.listMyCourses(principal.getUserId(), status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<InstructorCourseResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorCourseService.getMyCourse(principal.getUserId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<InstructorCourseResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody InstructorCourseUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorCourseService.update(principal.getUserId(), id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        instructorCourseService.delete(principal.getUserId(), id);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<GlobalApiResponse<InstructorCourseResponse>> publish(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorCourseService.publish(principal.getUserId(), id)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<GlobalApiResponse<Void>> archive(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        instructorCourseService.archive(principal.getUserId(), id);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<GlobalApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        instructorCourseCancellationService.cancelCourse(principal.getUserId(), id);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
