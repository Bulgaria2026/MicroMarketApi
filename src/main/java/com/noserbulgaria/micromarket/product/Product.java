package com.noserbulgaria.micromarket.product;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Audited
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class Product extends ExtendedEntity {

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Min(0)
  @Max(100)
  @Column(nullable = false)
  private int discount;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false)
  private long amount;
}
