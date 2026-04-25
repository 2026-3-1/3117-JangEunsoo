package com.jes.devlearn.domain.playback.controller;

import com.jes.devlearn.domain.playback.dto.request.PlaybackUpdateRequest;
import com.jes.devlearn.domain.playback.dto.response.PlaybackPositionResponse;
import com.jes.devlearn.domain.playback.dto.response.ResumeResponse;
import com.jes.devlearn.domain.playback.service.PlaybackService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackService playbackService;

    @PutMapping
    public ResponseEntity<GlobalApiResponse<PlaybackPositionResponse>> upsert(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PlaybackUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(playbackService.upsert(principal.getUserId(), req)));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<GlobalApiResponse<PlaybackPositionResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long lectureId,
            @RequestParam Long enrollmentId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(playbackService.get(principal.getUserId(), enrollmentId, lectureId)));
    }

    @GetMapping("/enrollments/{enrollmentId}/resume")
    public ResponseEntity<GlobalApiResponse<ResumeResponse>> resume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long enrollmentId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(playbackService.resume(principal.getUserId(), enrollmentId)));
    }
}
