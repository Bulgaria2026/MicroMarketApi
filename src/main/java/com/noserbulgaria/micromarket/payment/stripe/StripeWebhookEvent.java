package com.noserbulgaria.micromarket.payment.stripe;

public sealed interface StripeWebhookEvent {

  String eventId();

  String paymentIntentId();

  record PaymentSucceeded(String eventId, String paymentIntentId) implements StripeWebhookEvent {
  }

  record PaymentFailed(String eventId, String paymentIntentId) implements StripeWebhookEvent {
  }
}
