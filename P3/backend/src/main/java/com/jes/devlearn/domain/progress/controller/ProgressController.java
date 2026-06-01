package com.jes.devlearn.domain.progress.controller;

import com.jes.devlearn.domain.progress.dto.request.CompleteRequestDTO;
import com.jes.devlearn.domain.progress.service.ProgressService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/complete")
    public ResponseEntity<GlobalApiResponse<Void>> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteRequestDTO dto
    ) {
        progressService.complete(principal.getUserId(), dto);
        return ResponseEntity.ok(GlobalApiResponse.success("강의 항목이 완료 처리되었습니다."));
    }
}
