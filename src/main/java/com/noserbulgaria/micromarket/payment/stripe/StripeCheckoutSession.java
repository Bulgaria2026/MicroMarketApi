package com.noserbulgaria.micromarket.payment.stripe;

/** {@code id} is stored on the order for webhook lookup; {@code url} is handed to the frontend for redirect. */
public record StripeCheckoutSession(String id, String url) {
}
