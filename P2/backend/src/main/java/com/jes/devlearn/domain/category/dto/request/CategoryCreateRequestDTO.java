package com.jes.devlearn.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequestDTO(
        @NotBlank String name
) {}
