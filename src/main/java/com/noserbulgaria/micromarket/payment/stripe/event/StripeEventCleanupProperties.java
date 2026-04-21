package com.noserbulgaria.micromarket.payment.stripe.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stripe.event")
public class StripeEventCleanupProperties {

  private String cleanupCron = "0 0 * * * *";
  private Duration retention = Duration.ofDays(7);
}
