package com.noserbulgaria.micromarket.domain.customer;

import com.noserbulgaria.micromarket.domain.order.Order;
import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public abstract class Customer extends ExtendedEntity {

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "customer")
  private Set<Order> orders = new HashSet<>();
}
