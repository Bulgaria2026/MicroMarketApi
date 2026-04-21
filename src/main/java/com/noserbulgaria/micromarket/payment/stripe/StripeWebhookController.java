package com.noserbulgaria.micromarket.payment.stripe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Webhooks", description = "Stripe webhook receiver")
@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

  private static final String SIGNATURE_HEADER = "Stripe-Signature";

  private final StripeWebhookService stripeWebhookService;

  @PostMapping
  @Operation(summary = "Receive a signed Stripe webhook event")
  public void receive(
      @RequestBody String payload,
      @RequestHeader(SIGNATURE_HEADER) String signature
  ) {
    stripeWebhookService.process(payload, signature);
  }
}
