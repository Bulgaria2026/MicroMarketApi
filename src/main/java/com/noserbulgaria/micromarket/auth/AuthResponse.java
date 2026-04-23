package com.noserbulgaria.micromarket.auth;

public record AuthResponse(

    String accessToken,

    long expiresIn

) {
}
