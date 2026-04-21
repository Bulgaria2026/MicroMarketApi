package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderService;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.product.ProductRepository;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Settles a placed order once Stripe confirms the payment outcome. Receives the Stripe PaymentIntent id and performs
 * the order-side work: atomic stock decrement on success and transition to PAID; rollback + refund + CANCELLED if
 * stock evaporated between placement and settlement; transition to PAYMENT_FAILED on payment failure. The Stripe
 * webhook adapter calls this service rather than touching the order state directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSettlementService {

  private final OrderService orderService;
  private final ProductRepository productRepository;
  private final StripePaymentProvider stripePaymentProvider;

  @Transactional
  public void handlePaymentSucceeded(String stripePaymentIntentId) {
    Order order = orderService.findByStripePaymentIntentIdOrThrow(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring payment_succeeded for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    List<OrderItem> decremented = new ArrayList<>();
    for (OrderItem item : order.getOrderItems()) {
      int affected = productRepository.tryDecrementStock(item.getProduct().getId(), item.getQuantity());
      if (affected != 1) {
        rollbackDecrements(decremented);
        log.warn("Stock exhausted for product {} while settling order {} — cancelling + refunding",
            item.getProduct().getId(), order.getId());
        order.transitionTo(OrderStatusType.CANCELLED);
        stripePaymentProvider.refund(stripePaymentIntentId);
        return;
      }
      decremented.add(item);
    }
    order.transitionTo(OrderStatusType.PAID);
  }

  @Transactional
  public void handlePaymentFailed(String stripePaymentIntentId) {
    Order order = orderService.findByStripePaymentIntentIdOrThrow(stripePaymentIntentId);
    if (order.getStatus() != OrderStatusType.PENDING_PAYMENT) {
      log.info("Ignoring payment_failed for order {} (status {})", order.getId(), order.getStatus());
      return;
    }
    order.transitionTo(OrderStatusType.PAYMENT_FAILED);
  }

  private void rollbackDecrements(List<OrderItem> decremented) {
    for (OrderItem item : decremented) {
      productRepository.tryDecrementStock(item.getProduct().getId(), -item.getQuantity());
    }
  }
}
