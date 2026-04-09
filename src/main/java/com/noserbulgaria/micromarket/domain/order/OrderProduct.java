package com.noserbulgaria.micromarket.domain.order;

import java.math.BigDecimal;

import com.noserbulgaria.micromarket.generic.ExtendedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * JPA entity representing an product within an order.
 * Each product specifies the quantity and the price at the time of purchase.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "order_product")
public class OrderProduct extends ExtendedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // TODO: Add Product relationship here once the entity is created

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal priceAtPurchase;
}
