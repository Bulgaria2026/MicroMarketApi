package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import com.noserbulgaria.micromarket.order.Order;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** A buyer. Always has an email; gains a {@link Profile} once registered. */
@Data
@Entity
@Table(name = "customer")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Customer extends ExtendedEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(unique = true)
  private @Nullable String stripeCustomerId;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
  private @Nullable Profile profile;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "customer")
  private Set<Order> orders = new HashSet<>();

  public boolean isRegistered() {
    return profile != null;
  }

  public void setEmail(String email) {
    this.email = email.toLowerCase(Locale.ROOT);
  }
}
