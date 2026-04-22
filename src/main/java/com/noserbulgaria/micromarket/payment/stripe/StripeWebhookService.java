package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.checkout.OrderSettlementService;
import com.noserbulgaria.micromarket.payment.stripe.event.StripeEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Webhook intake: verify signature, dedup via {@link StripeEventStore}, dispatch exhaustively over the sealed event. */
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

  private final StripePaymentProvider stripePaymentProvider;
  private final StripeEventStore stripeEventStore;
  private final OrderSettlementService orderSettlementService;

  public void process(String payload, String signature) {
    stripePaymentProvider.verifyAndParse(payload, signature)
        .ifPresent(event -> stripeEventStore.onceFor(event, () -> dispatch(event)));
  }

  private void dispatch(StripeWebhookEvent event) {
    switch (event) {
      case StripeWebhookEvent.CheckoutSucceeded e ->
          orderSettlementService.handleCheckoutSucceeded(e.sessionId(), e.paymentIntentId());
      case StripeWebhookEvent.CheckoutFailed e -> orderSettlementService.handleCheckoutFailed(e.sessionId());
      case StripeWebhookEvent.CheckoutExpired e -> orderSettlementService.handleCheckoutExpired(e.sessionId());
    }
  }
}
