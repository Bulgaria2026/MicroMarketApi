package com.noserbulgaria.micromarket.domain.product;

import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Audited
@EqualsAndHashCode(callSuper = true)
@NullMarked
public class Product extends ExtendedEntity {

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(nullable = false)
  private BigDecimal price;

  @Min(0)
  @Max(100)
  @Column(nullable = false)
  private Integer discount;

  @Column(nullable = false)
  private Boolean enabled;

  @Column(nullable = false)
  private Long amount;
}
