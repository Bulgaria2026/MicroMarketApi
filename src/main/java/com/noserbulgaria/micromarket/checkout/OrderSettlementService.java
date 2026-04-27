package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.coupon.CouponService;
import com.noserbulgaria.micromarket.coupon.Coupon;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderService;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.payment.stripe.StripeCompletedCheckoutSession;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Settles an order once Stripe resolves the Checkout. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementService {

  private final OrderService orderService;
  private final ProductRepository productRepository;
  private final StripePaymentProvider stripePaymentProvider;
  private final CouponService couponService;

  @Transactional
  public void handleCheckoutSucceeded(String stripeCheckoutSessionId, String stripePaymentIntentId) {
    Order order = orderService.findByStripeCheckoutSessionIdOrThrow(stripeCheckoutSessionId);
    StripeCompletedCheckoutSession completedSession =
        stripePaymentProvider.retrieveCompletedCheckoutSession(stripeCheckoutSessionId);
    String resolvedPaymentIntentId = Objects.requireNonNullElse(completedSession.paymentIntentId(), stripePaymentIntentId);
    order.setStripePaymentIntentId(resolvedPaymentIntentId);
    order.setPaidTotal(completedSession.amountTotal());
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring checkout.succeeded for order {} (status {})", order.getId(), order.getStatus());
      return;
    }

    if (!reconcileCoupon(order, completedSession, resolvedPaymentIntentId)) {
      return;
    }

    List<OrderItem> decremented = new ArrayList<>();
    for (OrderItem item : order.getOrderItems()) {
      var product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElseThrow();
      if (product.getAmount() < item.getQuantity()) {
        rollbackDecrements(decremented);
        log.warn(
            "Stock exhausted for product {} while settling order {} — cancelling + refunding",
            item.getProduct().getId(), order.getId()
        );
        order.transitionTo(OrderStatusType.CANCELLED);
        stripePaymentProvider.refund(resolvedPaymentIntentId);
        return;
      }
      product.setAmount(product.getAmount() - item.getQuantity());
      decremented.add(item);
    }
    order.transitionTo(OrderStatusType.PAID);
  }

  @Transactional
  public void handleCheckoutFailed(String stripeCheckoutSessionId) {
    Order order = orderService.findByStripeCheckoutSessionIdOrThrow(stripeCheckoutSessionId);
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring checkout.failed for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.PAYMENT_FAILED);
  }

  @Transactional
  public void handleCheckoutExpired(String stripeCheckoutSessionId) {
    Order order = orderService.findByStripeCheckoutSessionIdOrThrow(stripeCheckoutSessionId);
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring checkout.expired for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.CANCELLED);
  }

  @Transactional
  public void handlePaymentRefunded(String stripePaymentIntentId) {
    Order order = orderService.findByStripePaymentIntentIdOrThrow(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.PAID) {
      log.info("Ignoring charge.refunded for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.REFUNDED);
    log.info(
        "Order {} ({}) transitioned to REFUNDED from payment intent {}",
        order.getId(), order.getOrderNumber(), stripePaymentIntentId
    );
  }

  @Transactional
  public void handlePaymentRefundFailed(String stripePaymentIntentId) {
    Order order = orderService.findByStripePaymentIntentIdOrThrow(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.REFUNDED) {
      log.info("Ignoring refund.failed for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.PAID);
    log.info(
        "Order {} ({}) reverted to PAID after refund failed on payment intent {}",
        order.getId(), order.getOrderNumber(), stripePaymentIntentId
    );
  }

  private void rollbackDecrements(List<OrderItem> decremented) {
    for (OrderItem item : decremented) {
      productRepository.findByIdForUpdate(item.getProduct().getId())
          .ifPresent(product -> product.setAmount(product.getAmount() + item.getQuantity()));
    }
  }

  private boolean reconcileCoupon(
      Order order,
      StripeCompletedCheckoutSession completedSession,
      String stripePaymentIntentId
  ) {
    String stripePromotionCodeId = completedSession.stripePromotionCodeId();
    if (stripePromotionCodeId == null) {
      order.setAppliedCoupon(null);
      order.setCouponCode(null);
      order.setCouponAmountOff(null);
      return true;
    }

    Coupon coupon = couponService.findByStripePromotionCodeIdSynced(stripePromotionCodeId)
        .orElse(null);
    if (coupon == null) {
      log.error(
          "Stripe Checkout session {} used unmanaged promotion code {} for order {} — cancelling + refunding",
          completedSession.id(), stripePromotionCodeId, order.getId()
      );
      order.transitionTo(OrderStatusType.CANCELLED);
      stripePaymentProvider.refund(stripePaymentIntentId);
      return false;
    }

    order.setAppliedCoupon(coupon);
    order.setCouponCode(coupon.getCode());
    order.setCouponAmountOff(completedSession.amountDiscount());
    return true;
  }
}
