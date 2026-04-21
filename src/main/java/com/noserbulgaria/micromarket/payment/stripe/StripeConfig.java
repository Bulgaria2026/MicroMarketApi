package com.noserbulgaria.micromarket.payment.stripe;

import com.stripe.StripeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@RequiredArgsConstructor
public class StripeConfig {

  private final StripeProperties properties;

  @Bean
  StripeClient stripeClient() {
    return new StripeClient(properties.secretKey());
  }
}
