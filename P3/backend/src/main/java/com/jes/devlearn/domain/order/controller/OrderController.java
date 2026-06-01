package com.jes.devlearn.domain.order.controller;

import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.order.service.OrderService;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<OrderResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(orderService.createFromCart(principal.getUserId())));
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<OrderResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderStatus status
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(orderService.listMine(principal.getUserId(), status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobalApiResponse<OrderResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(orderService.getMine(principal.getUserId(), id)));
    }
}
