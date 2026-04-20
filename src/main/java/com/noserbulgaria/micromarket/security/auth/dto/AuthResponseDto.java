package com.noserbulgaria.micromarket.security.auth.dto;

import java.util.UUID;

public record AuthResponseDto(

    String accessToken,

    long expiresIn,

    UUID profileId

) {
}
