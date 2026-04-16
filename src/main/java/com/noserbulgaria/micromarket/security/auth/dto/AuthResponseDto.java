package com.noserbulgaria.micromarket.security.auth.dto;

public record AuthResponseDto(

    String accessToken,

    String refreshToken,

    long expiresIn

) {
}
