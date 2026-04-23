package com.noserbulgaria.micromarket.customer.dto;

import com.noserbulgaria.micromarket.auth.user.dto.UserResponseDto;

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
