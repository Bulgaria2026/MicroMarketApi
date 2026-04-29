package com.noserbulgaria.micromarket.payment.stripe;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record StripeManagedCouponRequest(
    long amountOff,
    String code,
    @Nullable String name,
    boolean active,
    @Nullable Instant expiryDate,
    @Nullable Integer maxRedemptions,
    @Nullable String stripeCustomerId
) {
}
