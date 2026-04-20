package com.noserbulgaria.micromarket.security.user;

import org.jspecify.annotations.Nullable;

public record UserFilter(
    @Nullable String email,
    @Nullable Role role,
    @Nullable AccountStatus status
) {
}
