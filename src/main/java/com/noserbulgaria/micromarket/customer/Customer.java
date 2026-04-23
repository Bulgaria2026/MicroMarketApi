package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import com.noserbulgaria.micromarket.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

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

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "customer")
  private Set<Order> orders = new HashSet<>();
}
