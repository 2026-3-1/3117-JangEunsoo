package com.jes.devlearn.domain.admin.controller;

import com.jes.devlearn.domain.admin.dto.response.AdminOrderPageResponse;
import com.jes.devlearn.domain.admin.service.AdminOrderService;
import com.jes.devlearn.domain.order.dto.response.OrderResponse;
import com.jes.devlearn.domain.order.entity.OrderStatus;
import com.jes.devlearn.domain.payment.dto.request.RefundRequest;
import com.jes.devlearn.global.dto.GlobalApiResponse;
import com.jes.devlearn.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<AdminOrderPageResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(GlobalApiResponse.success(adminOrderService.list(status, pageable)));
    }

    @PostMapping("/{orderId}/refund")
    public ResponseEntity<GlobalApiResponse<OrderResponse>> forceRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest req
    ) {
        var orderItemIds = req == null ? null : req.orderItemIds();
        var reason = req == null ? null : req.reason();
        return ResponseEntity.ok(GlobalApiResponse.success(
                adminOrderService.forceRefund(principal.getUserId(), orderId, orderItemIds, reason)));
    }
}
