package com.jes.devlearn.domain.cart.dto.response;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        long totalAmount,
        int itemCount
) {
    public static CartResponse of(List<CartItemResponse> items) {
        long total = items.stream().mapToLong(i -> i.price() == null ? 0 : i.price()).sum();
        return new CartResponse(items, total, items.size());
    }
}
