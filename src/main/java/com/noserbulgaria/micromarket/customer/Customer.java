package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "customer_type")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public abstract class Customer extends ExtendedEntity {

  /**
   * Stripe Customer id ({@code cus_…}). Lazily filled on first checkout, so the Stripe dashboard shows a stable
   * per-person record across orders.
   */
  @Column(unique = true)
  private @Nullable String stripeCustomerId;
}
