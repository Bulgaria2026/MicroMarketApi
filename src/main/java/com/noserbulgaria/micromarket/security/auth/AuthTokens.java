package com.noserbulgaria.micromarket.security.auth;

public record AuthTokens(

    String accessToken,

    String refreshToken,

    long expiresIn

) {
}
