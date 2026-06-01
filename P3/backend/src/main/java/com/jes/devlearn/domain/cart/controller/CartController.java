package com.jes.devlearn.domain.cart.controller;

import com.jes.devlearn.domain.cart.dto.request.CartAddRequest;
import com.jes.devlearn.domain.cart.dto.response.CartItemResponse;
import com.jes.devlearn.domain.cart.dto.response.CartResponse;
import com.jes.devlearn.domain.cart.service.CartService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<CartResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(cartService.get(principal.getUserId())));
    }

    @PostMapping("/items")
    public ResponseEntity<GlobalApiResponse<CartItemResponse>> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CartAddRequest req
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(cartService.add(principal.getUserId(), req.courseId())));
    }

    @DeleteMapping("/items/{courseId}")
    public ResponseEntity<GlobalApiResponse<Void>> remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long courseId
    ) {
        cartService.remove(principal.getUserId(), courseId);
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }

    @DeleteMapping
    public ResponseEntity<GlobalApiResponse<Void>> clear(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        cartService.clear(principal.getUserId());
        return ResponseEntity.ok(GlobalApiResponse.success(null));
    }
}
