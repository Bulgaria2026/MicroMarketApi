package com.noserbulgaria.micromarket.payment.stripe.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Periodically purges Stripe webhook event records older than the configured retention. Stripe redelivers
 * unacknowledged events for up to 3 days, so anything older than the retention is no longer needed for dedup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeEventCleanupJob {

  private final StripeEventRepository stripeEventRepository;
  private final StripeEventCleanupProperties properties;

  @Scheduled(cron = "${stripe.event.cleanup-cron}")
  @Transactional
  public void purgeStale() {
    Instant cutoff = Instant.now().minus(properties.getRetention());
    int deleted = stripeEventRepository.deleteOlderThan(cutoff);
    if (deleted > 0) {
      log.info("Purged {} stripe event(s) older than {}", deleted, cutoff);
    }
  }
}
