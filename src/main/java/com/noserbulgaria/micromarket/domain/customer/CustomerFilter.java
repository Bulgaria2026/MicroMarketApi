package com.noserbulgaria.micromarket.domain.customer;

import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record CustomerFilter(
    @Nullable Instant createdFrom,
    @Nullable Instant createdTo,
    @Nullable String email,
    @Nullable Role role,
    @Nullable AccountStatus status,
    @Nullable Long minPoints,
    @Nullable Long maxPoints
) {
}
