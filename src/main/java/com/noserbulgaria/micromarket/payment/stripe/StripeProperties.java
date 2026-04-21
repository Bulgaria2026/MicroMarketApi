package com.noserbulgaria.micromarket.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
    String secretKey,
    String webhookSecret
) {
}
