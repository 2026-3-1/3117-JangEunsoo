package com.jes.devlearn.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequestDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 6, max = 14, message = "Username must be between 6 and 14 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username may contain only letters and numbers")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*]+$", message = "Password may contain only letters, numbers, and !@#$%^&*")
        String password
) {
}
