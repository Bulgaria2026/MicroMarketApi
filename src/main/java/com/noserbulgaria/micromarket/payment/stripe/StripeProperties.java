package com.noserbulgaria.micromarket.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
    String secretKey,
    String webhookSecret,
    String successUrl,
    String cancelUrl,
    @DefaultValue("30") int sessionExpirationMinutes
) {
}
