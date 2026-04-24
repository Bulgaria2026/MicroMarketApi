package com.noserbulgaria.micromarket.couponoffer;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponOfferResponse(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    @Nullable String description,

    @Nullable Instant startDate,

    @Nullable Instant expiryDate,

    int pointCost,

    BigDecimal amountOff,

    @Nullable Integer maxPurchases,

    int purchaseCount,

    boolean active
) {
}
