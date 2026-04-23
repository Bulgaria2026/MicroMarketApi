package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Facade over the Stripe SDK. EUR-only; amounts serialized as integer cents. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentProvider {

  public static final String CURRENCY = "eur";

  private static final String EVENT_CHECKOUT_COMPLETED = "checkout.session.completed";
  private static final String EVENT_CHECKOUT_ASYNC_SUCCEEDED = "checkout.session.async_payment_succeeded";
  private static final String EVENT_CHECKOUT_ASYNC_FAILED = "checkout.session.async_payment_failed";
  private static final String EVENT_CHECKOUT_EXPIRED = "checkout.session.expired";
  private static final String EVENT_CHARGE_REFUNDED = "charge.refunded";
  private static final String EVENT_REFUND_FAILED = "refund.failed";

  private static final String PAYMENT_STATUS_PAID = "paid";
  private static final String PAYMENT_STATUS_NO_PAYMENT_REQUIRED = "no_payment_required";
  private static final String PAYMENT_STATUS_UNPAID = "unpaid";

  private static final String METADATA_ORDER_NUMBER = "order_number";

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

  /** Idempotency-keyed on the order id, so a retried placement returns the same session instead of duplicating. */
  public StripeCheckoutSession createCheckoutSession(Order order, String stripeCustomerId) {
    SessionCreateParams.Builder builder = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .setCustomer(stripeCustomerId)
        .setClientReferenceId(order.getId().toString())
        .putMetadata(METADATA_ORDER_NUMBER, order.getOrderNumber())
        .setPaymentIntentData(
            SessionCreateParams.PaymentIntentData.builder()
                .putMetadata(METADATA_ORDER_NUMBER, order.getOrderNumber())
                .build())
        .setSuccessUrl(properties.successUrl())
        .setCancelUrl(properties.cancelUrl())
        .setExpiresAt(Instant.now().plus(Duration.ofMinutes(properties.sessionExpirationMinutes())).getEpochSecond());

    for (OrderItem item : order.getOrderItems()) {
      builder.addLineItem(
          SessionCreateParams.LineItem.builder()
              .setQuantity((long) item.getQuantity())
              .setPriceData(
                  SessionCreateParams.LineItem.PriceData.builder()
                      .setCurrency(CURRENCY)
                      .setUnitAmount(item.getPriceAtPurchase().movePointRight(2).longValueExact())
                      .setProductData(
                          SessionCreateParams.LineItem.PriceData.ProductData.builder()
                              .setName(item.getProductName())
                              .build())
                      .build())
              .build());
    }

    RequestOptions options = RequestOptions.builder()
        .setIdempotencyKey(order.getId().toString())
        .build();
    try {
      Session session = stripeClient.v1().checkout().sessions().create(builder.build(), options);
      return new StripeCheckoutSession(session.getId(), session.getUrl());
    } catch (StripeException ex) {
      throw new IllegalStateException(
          "Failed to create Stripe Checkout session for order %s".formatted(order.getId()), ex);
    }
  }

  /** Best-effort: logs failures but doesn't throw, so a Stripe outage can't roll back the CANCELLED transition. */
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

  /** Empty = ignored event (ack with 200). Throws {@link BadRequestApiException} on bad signature so Stripe retries. */
  public Optional<StripeWebhookEvent> verifyAndParse(String payload, String signatureHeader) {
    Event event;
    try {
      event = stripeClient.constructEvent(payload, signatureHeader, properties.webhookSecret());
    } catch (SignatureVerificationException _) {
      throw new BadRequestApiException("Invalid Stripe webhook signature");
    }
    return switch (event.getType()) {
      case EVENT_CHECKOUT_COMPLETED -> parseCompleted(event);
      case EVENT_CHECKOUT_ASYNC_SUCCEEDED -> {
        Session session = extractDataObject(event, Session.class);
        yield Optional.of(new StripeWebhookEvent.CheckoutSucceeded(
            event.getId(), session.getId(), session.getPaymentIntent()));
      }
      case EVENT_CHECKOUT_ASYNC_FAILED -> {
        Session session = extractDataObject(event, Session.class);
        yield Optional.of(new StripeWebhookEvent.CheckoutFailed(event.getId(), session.getId()));
      }
      case EVENT_CHECKOUT_EXPIRED -> {
        Session session = extractDataObject(event, Session.class);
        yield Optional.of(new StripeWebhookEvent.CheckoutExpired(event.getId(), session.getId()));
      }
      case EVENT_CHARGE_REFUNDED -> parseRefunded(event);
      case EVENT_REFUND_FAILED -> parseRefundFailed(event);
      default -> {
        log.debug("Ignoring Stripe event {} of unhandled type {}", event.getId(), event.getType());
        yield Optional.empty();
      }
    };
  }

  /** Async methods (bank transfers etc.) complete with payment_status=unpaid; wait for the async follow-up instead. */
  private Optional<StripeWebhookEvent> parseCompleted(Event event) {
    Session session = extractDataObject(event, Session.class);
    String paymentStatus = session.getPaymentStatus();
    return switch (paymentStatus) {
      case PAYMENT_STATUS_PAID, PAYMENT_STATUS_NO_PAYMENT_REQUIRED -> Optional.of(
          new StripeWebhookEvent.CheckoutSucceeded(event.getId(), session.getId(), session.getPaymentIntent()));
      case PAYMENT_STATUS_UNPAID -> {
        log.debug(
            "Checkout session {} completed with payment_status=unpaid — waiting for async outcome",
            session.getId()
        );
        yield Optional.empty();
      }
      default -> {
        log.warn(
            "Checkout session {} completed with unexpected payment_status={}",
            session.getId(), paymentStatus
        );
        yield Optional.empty();
      }
    };
  }

  /** Full refund only; partial refunds are logged and ignored. */
  private Optional<StripeWebhookEvent> parseRefunded(Event event) {
    Charge charge = extractDataObject(event, Charge.class);
    if (!Boolean.TRUE.equals(charge.getRefunded())) {
      log.info(
          "Ignoring partial refund on charge {} (amount_refunded={}, amount={})",
          charge.getId(), charge.getAmountRefunded(), charge.getAmount()
      );
      return Optional.empty();
    }
    String paymentIntentId = charge.getPaymentIntent();
    if (paymentIntentId == null) {
      log.warn("Charge {} fully refunded but has no payment_intent — cannot correlate to an order", charge.getId());
      return Optional.empty();
    }
    return Optional.of(new StripeWebhookEvent.PaymentRefunded(event.getId(), paymentIntentId));
  }

  /** Refund didn't complete; bank rejection or dashboard cancellation. */
  private Optional<StripeWebhookEvent> parseRefundFailed(Event event) {
    Refund refund = extractDataObject(event, Refund.class);
    String paymentIntentId = refund.getPaymentIntent();
    if (paymentIntentId == null) {
      log.warn("Refund {} failed but has no payment_intent — cannot correlate to an order", refund.getId());
      return Optional.empty();
    }
    return Optional.of(new StripeWebhookEvent.PaymentRefundFailed(event.getId(), paymentIntentId));
  }

  private <T extends StripeObject> T extractDataObject(Event event, Class<T> type) {
    StripeObject data = event.getDataObjectDeserializer().getObject()
        .orElseThrow(() -> new IllegalStateException(
            "Stripe event %s has no deserializable data object".formatted(event.getId())));
    if (!type.isInstance(data)) {
      throw new IllegalStateException(
          "Stripe event %s of type %s did not carry a %s".formatted(
              event.getId(), event.getType(), type.getSimpleName()));
    }
    return type.cast(data);
  }
}
