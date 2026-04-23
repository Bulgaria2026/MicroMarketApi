package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record CustomerFilter(
    @Nullable Instant createdFrom,
    @Nullable Instant createdTo,
    @Nullable String email,
    @Nullable CustomerType type,
    @Nullable Role role,
    @Nullable AccountStatus status,
    @Nullable Long minPoints,
    @Nullable Long maxPoints
) {
}
