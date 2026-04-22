package com.noserbulgaria.micromarket.payment.stripe;

public sealed interface StripeWebhookEvent {

  String eventId();

  record CheckoutSucceeded(String eventId, String sessionId, String paymentIntentId) implements StripeWebhookEvent {
  }

  record CheckoutFailed(String eventId, String sessionId) implements StripeWebhookEvent {
  }

  record CheckoutExpired(String eventId, String sessionId) implements StripeWebhookEvent {
  }
}
