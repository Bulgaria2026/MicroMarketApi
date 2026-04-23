package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(

    UUID id,

    UserResponse user,

    long points,

    Instant createdAt,

    Instant updatedAt

) {
}
