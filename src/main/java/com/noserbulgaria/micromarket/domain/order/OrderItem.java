package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.domain.product.Product;
import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("NullAway.Init")
public class OrderItem extends ExtendedEntity {

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal priceAtPurchase;
}
