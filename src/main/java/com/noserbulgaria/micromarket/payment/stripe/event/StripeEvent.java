package com.noserbulgaria.micromarket.payment.stripe.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@SuppressWarnings("NullAway.Init")
public class StripeEvent {

  @Id
  @Column(nullable = false)
  private String eventId;

  @Column(nullable = false, length = 100)
  private String eventType;

  @Column(nullable = false)
  private Instant receivedAt;
}
