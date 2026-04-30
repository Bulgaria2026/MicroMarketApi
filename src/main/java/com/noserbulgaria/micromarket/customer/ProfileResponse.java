package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountResponse;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(

    UUID id,

    AccountResponse user,

    long points,

    Instant createdAt,

    Instant updatedAt

) {
}
