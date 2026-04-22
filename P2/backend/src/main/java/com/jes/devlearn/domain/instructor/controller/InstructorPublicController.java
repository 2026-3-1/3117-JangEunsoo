package com.jes.devlearn.domain.instructor.controller;

import com.jes.devlearn.domain.instructor.dto.response.InstructorPublicProfileResponse;
import com.jes.devlearn.domain.instructor.service.InstructorProfileService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
public class InstructorPublicController {

    private final InstructorProfileService instructorProfileService;

    @GetMapping("/{userId}")
    public ResponseEntity<GlobalApiResponse<InstructorPublicProfileResponse>> getPublic(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(instructorProfileService.getPublic(userId)));
    }
}
