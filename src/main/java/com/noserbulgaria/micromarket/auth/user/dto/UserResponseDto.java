package com.noserbulgaria.micromarket.auth.user.dto;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String email,

    Role role,

    AccountStatus status

) {
}
