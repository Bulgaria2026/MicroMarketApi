package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentProvider {

  private static final String EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
  private static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";

  private final StripeClient stripeClient;
  private final StripeProperties properties;

  public String createCustomer(String email) {
    CustomerCreateParams params = CustomerCreateParams.builder()
        .setEmail(email)
        .build();
    try {
      return stripeClient.v1().customers().create(params).getId();
    } catch (StripeException ex) {
      throw new IllegalStateException(
          "Failed to create Stripe customer for %s".formatted(email), ex);
    }
  }

  public StripePaymentIntent initiate(
      UUID orderId,
      BigDecimal amount,
      String currency,
      @Nullable String stripeCustomerId
  ) {
    PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
        .setAmount(toMinorUnits(amount))
        .setCurrency(currency.toLowerCase())
        .putMetadata("order_id", orderId.toString())
        .setAutomaticPaymentMethods(
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                .build()
        );
    if (stripeCustomerId != null) {
      builder.setCustomer(stripeCustomerId);
    }
    RequestOptions options = RequestOptions.builder()
        .setIdempotencyKey(orderId.toString())
        .build();
    try {
      PaymentIntent intent = stripeClient.v1().paymentIntents().create(builder.build(), options);
      return new StripePaymentIntent(intent.getId(), intent.getClientSecret());
    } catch (StripeException ex) {
      throw new IllegalStateException(
          "Failed to create Stripe payment intent for order %s".formatted(orderId), ex);
    }
  }

  /**
   * Best-effort refund. A failure here is logged at ERROR for operational alerting but not thrown: by the time we
   * refund, the order is already in {@code CANCELLED} and the customer's money is still on Stripe's side
   */
  public void refund(String paymentIntentId) {
    RefundCreateParams params = RefundCreateParams.builder()
        .setPaymentIntent(paymentIntentId)
        .build();
    try {
      stripeClient.v1().refunds().create(params);
    } catch (StripeException ex) {
      log.error("Stripe refund failed for payment intent {} — requires manual intervention", paymentIntentId, ex);
    }
  }

  /**
   * Verifies the webhook signature and parses the event. Returns empty when the event type is one we don't handle —
   * callers ack those with 200 and move on. For handled types the result carries a non-null payment intent id.
   */
  public Optional<StripeWebhookEvent> verifyAndParse(String payload, String signatureHeader) {
    Event event;
    try {
      event = stripeClient.constructEvent(payload, signatureHeader, properties.webhookSecret());
    } catch (SignatureVerificationException _) {
      throw new BadRequestApiException("Invalid Stripe webhook signature");
    }
    return switch (event.getType()) {
      case EVENT_PAYMENT_SUCCEEDED -> Optional.of(
          new StripeWebhookEvent.PaymentSucceeded(event.getId(), extractPaymentIntentId(event)));
      case EVENT_PAYMENT_FAILED -> Optional.of(
          new StripeWebhookEvent.PaymentFailed(event.getId(), extractPaymentIntentId(event)));
      default -> {
        log.debug("Ignoring Stripe event {} of unhandled type {}", event.getId(), event.getType());
        yield Optional.empty();
      }
    };
  }

  private String extractPaymentIntentId(Event event) {
    StripeObject data = event.getDataObjectDeserializer().getObject()
        .orElseThrow(() -> new IllegalStateException(
            "Stripe event %s has no deserializable data object".formatted(event.getId())));
    if (!(data instanceof PaymentIntent intent)) {
      throw new IllegalStateException(
          "Stripe event %s of type %s did not carry a PaymentIntent".formatted(event.getId(), event.getType()));
    }
    return intent.getId();
  }

  private static long toMinorUnits(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
  }
}
