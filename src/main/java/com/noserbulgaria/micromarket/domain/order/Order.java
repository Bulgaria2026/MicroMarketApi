package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "orders")
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("NullAway.Init")
public class Order extends ExtendedEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatusType status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<OrderItem> orderItems = new HashSet<>();

  // TODO: Add PaymentDetails relationship here once the entity is created
}
