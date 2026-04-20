package com.noserbulgaria.micromarket.security.auth;

import java.util.UUID;

public record AuthTokens(

    String accessToken,

    String refreshToken,

    long expiresIn,

    UUID profileId

) {
}
