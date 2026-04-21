package com.noserbulgaria.micromarket.payment.stripe;

public record StripePaymentIntent(
    String id,
    String clientSecret
) {
}
