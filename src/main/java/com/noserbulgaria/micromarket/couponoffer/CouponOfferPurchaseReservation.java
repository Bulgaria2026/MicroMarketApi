package com.noserbulgaria.micromarket.couponoffer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CouponOfferPurchaseReservation(
    UUID couponOfferId,
    UUID userId,
    UUID stripeCustomerOwnerId,
    String stripeCustomerEmail,
    String code,
    String name,
    @Nullable Instant expiryDate,
    int pointCost,
    BigDecimal amountOff,
    boolean deactivatedOffer
) {
}
