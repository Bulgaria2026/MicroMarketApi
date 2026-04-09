package com.noserbulgaria.micromarket.domain.stock;

import com.noserbulgaria.micromarket.domain.product.Product;
import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true, exclude = "products")
public class Stock extends ExtendedEntity {

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "stock_product",
      joinColumns = @JoinColumn(name = "stock_id"),
      inverseJoinColumns = @JoinColumn(name = "product_id")
  )
  private Set<Product> products;

  @Column(nullable = false)
  private Long amount;
}
