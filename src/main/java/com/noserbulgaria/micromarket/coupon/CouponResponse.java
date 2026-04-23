package com.noserbulgaria.micromarket.coupon;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    @Nullable UUID userId,

    String code,

    @Nullable String name,

    @Nullable Instant startDate,

    @Nullable Instant expiryDate,

    int pointCost,

    BigDecimal amountOff,

    @Nullable Integer maxRedemptions,

    int timesRedeemed,

    boolean active
) {
}
