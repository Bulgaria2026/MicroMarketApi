package com.noserbulgaria.micromarket.security.user.dto;

import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;

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
