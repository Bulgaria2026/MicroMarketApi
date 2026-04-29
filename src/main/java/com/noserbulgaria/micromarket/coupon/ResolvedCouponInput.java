package com.noserbulgaria.micromarket.coupon;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResolvedCouponInput(
    @Nullable UUID userId,
    @Nullable UUID stripeCustomerOwnerId,
    @Nullable String stripeCustomerEmail,
    String code,
    @Nullable String name,
    @Nullable Instant expiryDate,
    int pointCost,
    BigDecimal amountOff,
    @Nullable Integer maxRedemptions,
    boolean active
) {
}
