package com.noserbulgaria.micromarket.domain.order;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.noserbulgaria.micromarket.generic.ExtendedEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "orders")
@EqualsAndHashCode(callSuper = false)
public class Order extends ExtendedEntity {

  @Enumerated(EnumType.STRING)
  private OrderStatusType status;

  @Column(nullable = false)
  private UUID customerId;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<OrderItem> orderItems = new HashSet<>();

  // TODO: Add PaymentDetails relationship here once the entity is created
}
