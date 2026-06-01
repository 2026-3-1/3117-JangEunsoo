package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.instructor.dto.response.InstructorCourseStudentsResponse;
import com.jes.devlearn.domain.instructor.dto.response.InstructorDashboardResponse;
import com.jes.devlearn.domain.instructor.service.InstructorDashboardService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorDashboardController {

    private final InstructorDashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<GlobalApiResponse<InstructorDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(dashboardService.getDashboard(principal.getUserId())));
    }

    @GetMapping("/courses/{id}/students")
    public ResponseEntity<GlobalApiResponse<InstructorCourseStudentsResponse>> getCourseStudents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(dashboardService.getCourseStudents(principal.getUserId(), id)));
    }
}
