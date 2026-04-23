package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.payment.stripe.StripeCheckoutSession;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Non-transactional orchestrator — the Stripe call runs between transactions so it holds no DB locks. */
@Service
@RequiredArgsConstructor
public class OrderPlacementService {

  private final OrderPlacementTransactions transactions;
  private final StripePaymentProvider stripePaymentProvider;

  public PlaceOrderResponse placeOrder(PlaceOrderRequest request, @Nullable CustomUserDetails userDetails) {
    Order order = transactions.createPendingOrder(request, userDetails);
    String stripeCustomerId = transactions.ensureStripeCustomer(order.getCustomer().getId(), order.getEmail());
    StripeCheckoutSession session = stripePaymentProvider.createCheckoutSession(order, stripeCustomerId);
    transactions.attachCheckoutSession(order.getId(), session.id());
    return new PlaceOrderResponse(order.getId(), order.getOrderNumber(), order.getTotalAmount(), session.url());
  }
}
