package com.noserbulgaria.micromarket.payment.stripe;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record StripeCompletedCheckoutSession(
    String id,
    @Nullable String paymentIntentId,
    BigDecimal amountSubtotal,
    BigDecimal amountTotal,
    @Nullable String stripePromotionCodeId,
    BigDecimal amountDiscount
) {
}
