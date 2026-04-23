package com.noserbulgaria.micromarket.payment.stripe.event;

import com.noserbulgaria.micromarket.payment.stripe.StripeWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * At-most-once execution for Stripe webhook events. Persists the event id with a unique constraint, then runs
 * {@code action}. If the action throws, the dedup row is rolled back so the next Stripe redelivery retries — never
 * silently swallows a failed processing attempt.
 *
 * @see <a href="https://docs.stripe.com/webhooks#handle-duplicate-events">Stripe Webhook Documentation</a>
 */
@Component
@RequiredArgsConstructor
public class StripeEventStore {

  private final StripeEventRepository stripeEventRepository;

  @Transactional
  public void onceFor(StripeWebhookEvent event, Runnable action) {
    StripeEvent entry = new StripeEvent();
    entry.setEventId(event.eventId());
    entry.setEventType(event.getClass().getSimpleName());
    entry.setReceivedAt(Instant.now());
    try {
      stripeEventRepository.saveAndFlush(entry);
    } catch (DataIntegrityViolationException _) {
      return;
    }
    action.run();
  }
}
