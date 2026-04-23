package com.noserbulgaria.micromarket.customer.dto;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.customer.CustomerType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponseDto(

    UUID id,

    CustomerType type,

    Instant createdAt,

    Instant updatedAt,

    String email,

    @Nullable Role role,

    @Nullable AccountStatus status,

    @Nullable Long points

) {
}
