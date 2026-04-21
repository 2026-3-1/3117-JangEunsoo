package com.jes.devlearn.domain.review.controller;

import com.jes.devlearn.domain.review.dto.request.ReviewCreateRequestDTO;
import com.jes.devlearn.domain.review.dto.response.ReviewResponseDTO;
import com.jes.devlearn.domain.review.service.ReviewService;
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
@RequestMapping("api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<ReviewResponseDTO>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewCreateRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.success(reviewService.createReview(principal.getUserId(), dto)));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<GlobalApiResponse<List<ReviewResponseDTO>>> getReviews(
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(reviewService.getReviewsByCourse(courseId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(id, principal.getUserId());
        return ResponseEntity.ok(GlobalApiResponse.success("리뷰가 삭제되었습니다."));
    }
}
