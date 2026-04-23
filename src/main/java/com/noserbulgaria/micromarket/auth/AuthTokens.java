package com.noserbulgaria.micromarket.auth;

public record AuthTokens(

    String accessToken,

    String refreshToken,

    long expiresIn

) {
}
