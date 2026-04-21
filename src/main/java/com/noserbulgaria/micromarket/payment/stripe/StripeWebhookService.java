package com.noserbulgaria.micromarket.payment.stripe;

import com.noserbulgaria.micromarket.checkout.OrderSettlementService;
import com.noserbulgaria.micromarket.payment.stripe.event.StripeEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates Stripe webhook intake: signature verification, dedup-with-rollback-on-failure, and dispatch to the
 * order-side settlement use case.
 */
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
      case StripeWebhookEvent.PaymentSucceeded p -> orderSettlementService.handlePaymentSucceeded(p.paymentIntentId());
      case StripeWebhookEvent.PaymentFailed p -> orderSettlementService.handlePaymentFailed(p.paymentIntentId());
    }
  }
}
