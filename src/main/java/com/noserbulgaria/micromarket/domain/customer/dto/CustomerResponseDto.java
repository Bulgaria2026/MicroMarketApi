package com.noserbulgaria.micromarket.domain.customer.dto;

import com.noserbulgaria.micromarket.domain.customer.CustomerType;
import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;
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
