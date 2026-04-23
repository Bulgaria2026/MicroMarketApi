package com.noserbulgaria.micromarket.domain.profile.dto;

import com.noserbulgaria.micromarket.security.user.dto.UserResponseDto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponseDto(

    UUID id,

    UserResponseDto user,

    long points,

    Instant createdAt,

    Instant updatedAt

) {
}
