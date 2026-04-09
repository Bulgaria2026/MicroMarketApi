package com.noserbulgaria.micromarket.security.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {

}
