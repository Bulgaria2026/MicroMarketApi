package com.noserbulgaria.micromarket.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponseDTO(
    @Schema(description = "Short-lived JWT for API access")
    String accessToken,

    @Schema(description = "Long-lived JWT used to obtain new access tokens")
    String refreshToken,

    @Schema(description = "Access token lifetime in seconds", example = "900")
    long expiresIn
) {

}
