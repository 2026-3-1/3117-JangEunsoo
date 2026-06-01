package com.jes.devlearn.domain.auth.dto.response;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken
) {
}
