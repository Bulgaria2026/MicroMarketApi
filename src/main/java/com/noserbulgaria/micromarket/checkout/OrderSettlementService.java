package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.mail.order.OrderConfirmedEvent;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderService;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Settles an order once Stripe resolves the Checkout. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementService {

  private final OrderService orderService;
  private final ProductRepository productRepository;
  private final StripePaymentProvider stripePaymentProvider;
  private final ApplicationEventPublisher events;

  @Transactional
  public void handleCheckoutSucceeded(String stripeCheckoutSessionId, String stripePaymentIntentId) {
    Order order = orderService.findByStripeCheckoutSessionIdOrThrow(stripeCheckoutSessionId);
    order.setStripePaymentIntentId(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring checkout.succeeded for order {} (status {})", order.getId(), order.getStatus());
      return;
    }

    List<OrderItem> decremented = new ArrayList<>();
    for (OrderItem item : order.getOrderItems()) {
      int affected = productRepository.tryDecrementStock(item.getProduct().getId(), item.getQuantity());
      if (affected != 1) {
        rollbackDecrements(decremented);
        log.warn(
            "Stock exhausted for product {} while settling order {} — cancelling + refunding",
            item.getProduct().getId(), order.getId()
        );
        order.transitionTo(OrderStatusType.CANCELLED);
        stripePaymentProvider.refund(stripePaymentIntentId);
        return;
      }
      decremented.add(item);
    }
    order.transitionTo(OrderStatusType.PAID);
    events.publishEvent(new OrderConfirmedEvent(order.getId()));
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
    log.info("Order {} ({}) transitioned to REFUNDED from payment intent {}",
        order.getId(), order.getOrderNumber(), stripePaymentIntentId);
  }

  @Transactional
  public void handlePaymentRefundFailed(String stripePaymentIntentId) {
    Order order = orderService.findByStripePaymentIntentIdOrThrow(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.REFUNDED) {
      log.info("Ignoring refund.failed for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.PAID);
    log.info("Order {} ({}) reverted to PAID after refund failed on payment intent {}",
        order.getId(), order.getOrderNumber(), stripePaymentIntentId);
  }

  private void rollbackDecrements(List<OrderItem> decremented) {
    for (OrderItem item : decremented) {
      productRepository.tryDecrementStock(item.getProduct().getId(), -item.getQuantity());
    }
  }
}
