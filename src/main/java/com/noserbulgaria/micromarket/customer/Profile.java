package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

@Data
@Entity
@Table(name = "profile")
@DiscriminatorValue("PROFILE")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Profile extends Customer {

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private int points;

  /**
   * PSP-side customer reference (e.g. Stripe Customer id). Lazily filled on first authenticated checkout so the
   * Stripe dashboard shows a stable customer link instead of one-off PaymentIntents. Unblocks saved cards / Stripe
   * Tax / dispute correlation later without a backfill.
   */
  @Column(name = "stripe_customer_id", unique = true, length = 64)
  private @Nullable String stripeCustomerId;
}
