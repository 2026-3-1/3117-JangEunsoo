package com.jes.devlearn.domain.category.dto.response;

import com.jes.devlearn.domain.category.entity.Category;

public record CategoryResponseDTO(
        Long id,
        String name
) {
    public static CategoryResponseDTO from(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName()
        );
    }
}
