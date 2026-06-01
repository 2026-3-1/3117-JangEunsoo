package com.jes.devlearn.domain.bookmark.controller;

import com.jes.devlearn.domain.bookmark.dto.request.BookmarkCreateRequest;
import com.jes.devlearn.domain.bookmark.dto.request.BookmarkUpdateRequest;
import com.jes.devlearn.domain.bookmark.dto.response.BookmarkResponse;
import com.jes.devlearn.domain.bookmark.service.BookmarkService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<BookmarkResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long lectureId
    ) {
        if (lectureId != null) {
            return ResponseEntity.ok(GlobalApiResponse.success(bookmarkService.listByLecture(principal.getUserId(), lectureId)));
        }
        return ResponseEntity.ok(GlobalApiResponse.success(bookmarkService.listMine(principal.getUserId())));
    }

    @PostMapping
    public ResponseEntity<GlobalApiResponse<BookmarkResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookmarkCreateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(bookmarkService.create(principal.getUserId(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<BookmarkResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody BookmarkUpdateRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(bookmarkService.update(principal.getUserId(), id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        bookmarkService.delete(principal.getUserId(), id);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
