package com.noserbulgaria.micromarket.security.auth.dto;

public record AuthResponseDTO(

    String accessToken,

    String refreshToken,

    long expiresIn

) {
}
