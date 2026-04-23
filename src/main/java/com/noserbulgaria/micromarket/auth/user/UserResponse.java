package com.noserbulgaria.micromarket.auth.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String email,

    Role role,

    AccountStatus status

) {
}
