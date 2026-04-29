package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.common.ExtendedEntity;
import com.noserbulgaria.micromarket.coupon.Coupon;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Table(name = "orders")
@Audited
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("NullAway.Init")
public class Order extends ExtendedEntity {

  @Column(nullable = false, unique = true, length = 32)
  private String orderNumber;

  @Setter(AccessLevel.NONE)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatusType status;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private @Nullable Coupon appliedCoupon;

  @Column(length = 500)
  private @Nullable String couponCode;

  @Column(precision = 10, scale = 2)
  private @Nullable BigDecimal couponAmountOff;

  /**
   * Snapshot of the recipient email at order time. Survives later customer-record changes so receipts remain
   * reconstructible.
   */
  @Column(nullable = false)
  private String email;

  /** Stripe Checkout Session id ({@code cs_…}); lookup key on webhook arrival. */
  @Column(unique = true)
  private @Nullable String stripeCheckoutSessionId;

  /** Stripe Payment Intent id ({@code pi_…}) */
  @Column(unique = true)
  private @Nullable String stripePaymentIntentId;

  /** The paid amount w/o any discounts or coupons applied before stripe confirmation */
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal subtotal;

  /** Actually paid amount, confirmed by stripe */
  @Column(precision = 10, scale = 2)
  private @Nullable BigDecimal paidTotal;

  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<OrderItem> orderItems = new HashSet<>();

  @Column(nullable = false)
  @Builder.Default
  private boolean pointsAwarded = false;

  /**
   * Transitions the order to {@code next} if {@link OrderStatusType#canTransitionTo} allows it. No-op when already
   * in the target state.
   */
  public void transitionTo(OrderStatusType next) {
    if (this.status == next) {
      return;
    }
    if (!this.status.canTransitionTo(next)) {
      throw new ConflictApiException(
          "Illegal order status transition: %s -> %s".formatted(this.status, next));
    }
    this.status = next;
  }
}
