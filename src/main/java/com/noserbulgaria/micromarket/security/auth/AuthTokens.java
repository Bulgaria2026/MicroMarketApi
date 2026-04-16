package com.noserbulgaria.micromarket.security.auth;

record AuthTokens(

    String accessToken,

    String refreshToken,

    long expiresIn

) {
}
