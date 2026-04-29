package com.noserbulgaria.micromarket.payment.stripe;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record StripePromotionCodeRequest(
    String code,
    boolean active,
    @Nullable Instant expiryDate,
    @Nullable Integer maxRedemptions,
    @Nullable String stripeCustomerId
) {
}
