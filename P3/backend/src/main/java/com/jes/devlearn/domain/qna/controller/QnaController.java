package com.jes.devlearn.domain.qna.controller;

import com.jes.devlearn.domain.qna.dto.request.QnaAnswerRequest;
import com.jes.devlearn.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.jes.devlearn.domain.qna.dto.request.QnaQuestionUpdateRequest;
import com.jes.devlearn.domain.qna.dto.response.QnaAnswerResponse;
import com.jes.devlearn.domain.qna.dto.response.QnaQuestionPageResponse;
import com.jes.devlearn.domain.qna.dto.response.QnaQuestionResponse;
import com.jes.devlearn.domain.qna.service.QnaService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    // ---- 강의 단위 질문 목록/작성 ----

    @GetMapping("/courses/{courseId}/questions")
    public ResponseEntity<GlobalApiResponse<QnaQuestionPageResponse>> listByCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.listByCourse(principal.getUserId(), principal.getRole(), courseId, pageable)));
    }

    @PostMapping("/courses/{courseId}/questions")
    public ResponseEntity<GlobalApiResponse<QnaQuestionResponse>> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody QnaQuestionCreateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.createQuestion(principal.getUserId(), courseId, req)));
    }

    // ---- 질문 상세/수정/삭제 ----

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<GlobalApiResponse<QnaQuestionResponse>> getQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.getQuestion(principal.getUserId(), principal.getRole(), questionId)));
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<GlobalApiResponse<QnaQuestionResponse>> updateQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId,
            @Valid @RequestBody QnaQuestionUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.updateQuestion(principal.getUserId(), questionId, req)));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId
    ) {
        qnaService.deleteQuestion(principal.getUserId(), principal.getRole(), questionId);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }

    // ---- 답변 작성/수정/삭제 ----

    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<GlobalApiResponse<QnaAnswerResponse>> createAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long questionId,
            @Valid @RequestBody QnaAnswerRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.createAnswer(principal.getUserId(), principal.getRole(), questionId, req)));
    }

    @PutMapping("/answers/{answerId}")
    public ResponseEntity<GlobalApiResponse<QnaAnswerResponse>> updateAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long answerId,
            @Valid @RequestBody QnaAnswerRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(
                qnaService.updateAnswer(principal.getUserId(), principal.getRole(), answerId, req)));
    }

    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<GlobalApiResponse<Void>> deleteAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long answerId
    ) {
        qnaService.deleteAnswer(principal.getUserId(), principal.getRole(), answerId);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
