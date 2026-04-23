package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(

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
