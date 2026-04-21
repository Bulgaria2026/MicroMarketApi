package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import com.noserbulgaria.micromarket.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Getter
@ToString
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Audited
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class OrderItem extends ExtendedEntity {

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  /**
   * Live FK to the product. Historical reads should prefer the {@link #productName} snapshot below — the FK is for
   * live joins.
   */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  private int quantity;

  /** Effective price charged per unit (after discount). */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal priceAtPurchase;

  /**
   * Pre-discount unit price at order time. Lets receipts render "you saved €X" and lets analytics report on discount
   * uptake without joining Product revisions. Equal to {@link #priceAtPurchase} when no discount applied.
   */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal originalUnitPrice;

  /**
   * Snapshot of the product name at order time. Required because invoices are legal documents that must render from
   * one row — Envers reconstruction is "archaeology, not the invoice."
   */
  @Column(nullable = false)
  private String productName;
}
