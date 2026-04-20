package com.noserbulgaria.micromarket.domain.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponseDto(

    UUID id,

    UUID userId,

    long points,

    Instant createdAt,

    Instant updatedAt

) {
}
