package com.noserbulgaria.micromarket.couponoffer;

import com.noserbulgaria.micromarket.common.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Table(name = "coupon_offer")
@Audited
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("NullAway.Init")
public class CouponOffer extends ExtendedEntity {

  @Column(nullable = false, length = 80)
  private String name;

  @Column(length = 500)
  private @Nullable String description;

  private @Nullable Instant startDate;

  private @Nullable Instant expiryDate;

  @Min(0)
  @Column(nullable = false)
  private int pointCost;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amountOff;

  private @Nullable Integer maxPurchases;

  @Column(nullable = false)
  @Builder.Default
  private int purchaseCount = 0;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

}
