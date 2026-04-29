package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.common.ExtendedEntity;
import com.noserbulgaria.micromarket.couponoffer.CouponOffer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Table(name = "coupon")
@Audited
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("NullAway.Init")
public class Coupon extends ExtendedEntity {

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private @Nullable User user;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_offer_id")
  private @Nullable CouponOffer couponOffer;

  @Column(nullable = false, unique = true, length = 500)
  private String code;

  @Column(length = 40)
  private @Nullable String name;

  private @Nullable Instant expiryDate;

  @Min(0)
  @Column(nullable = false)
  private int pointCost;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amountOff;

  private @Nullable Integer maxRedemptions;

  @Column(nullable = false)
  @Builder.Default
  private int timesRedeemed = 0;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(nullable = false)
  private String stripeCouponId;

  @Column(nullable = false, unique = true)
  private String stripePromotionCodeId;
}
