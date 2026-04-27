package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.customer.CustomerResolver;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderNumberGenerator;
import com.noserbulgaria.micromarket.order.OrderRepository;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Split into a separate bean so each {@code @Transactional} goes through the proxy (self-invocation wouldn't). */
@Service
@RequiredArgsConstructor
public class OrderPlacementTransactions {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final CustomerRepository customerRepository;
  private final CustomerResolver customerResolver;
  private final StripePaymentProvider stripePaymentProvider;
  private final OrderNumberGenerator orderNumberGenerator;

  private record ResolvedItem(Product product, int quantity) {
  }

  /** Returned Order is detached; its {@code orderItems} collection is in-memory so Stripe can read it outside the tx. */
  @Transactional
  public Order createPendingOrder(PlaceOrderRequest request, @Nullable CustomUserDetails userDetails) {
    List<ResolvedItem> items = resolveItems(request.items());
    Customer customer = customerResolver.resolveForCheckout(userDetails, request.email());
    String email = resolveEmail(userDetails, request);

    BigDecimal subtotal = items.stream()
        .map(this::itemTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    Order order = Order.builder()
        .orderNumber(orderNumberGenerator.next())
        .status(OrderStatusType.PENDING_PAYMENT)
        .customer(customer)
        .email(email)
        .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
        .build();
    for (ResolvedItem item : items) {
      order.getOrderItems().add(toOrderItem(order, item));
    }
    return orderRepository.saveAndFlush(order);
  }

  /** Lazily creates and caches a Stripe Customer on the {@code customer} row so repeat orders reuse it. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String ensureStripeCustomer(UUID customerId, String email) {
    Customer customer = customerRepository.findById(customerId).orElseThrow();
    var existing = customer.getStripeCustomerId();
    if (existing != null) return existing;
    String stripeCustomerId = stripePaymentProvider.createCustomer(email);
    customer.setStripeCustomerId(stripeCustomerId);
    customerRepository.saveAndFlush(customer);
    return stripeCustomerId;
  }

  /** No-ops if the session id is already set — matches Stripe's own idempotency-key replay semantics. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void attachCheckoutSession(UUID orderId, String stripeCheckoutSessionId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    if (order.getStripeCheckoutSessionId() != null) {
      return;
    }
    order.setStripeCheckoutSessionId(stripeCheckoutSessionId);
    orderRepository.saveAndFlush(order);
  }

  private String resolveEmail(@Nullable CustomUserDetails userDetails, PlaceOrderRequest request) {
    if (userDetails != null) {
      return userDetails.user().getEmail();
    }
    String requestEmail = request.email();
    if (requestEmail == null || requestEmail.isBlank()) {
      throw new BadRequestApiException("Email is required for guest checkout");
    }
    return requestEmail.trim();
  }

  private List<ResolvedItem> resolveItems(List<PlaceOrderItem> requested) {
    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (PlaceOrderItem item : requested) {
      quantities.merge(item.productId(), item.quantity(), Integer::sum);
    }

    List<Product> products = productRepository.findByIdInAndEnabledTrue(quantities.keySet());
    if (products.size() != quantities.size()) {
      throw new BadRequestApiException("One or more products do not exist");
    }

    List<ResolvedItem> resolved = new ArrayList<>(products.size());
    for (Product product : products) {
      int quantity = Objects.requireNonNull(quantities.get(product.getId()));
      ensureAvailable(product, quantity);
      resolved.add(new ResolvedItem(product, quantity));
    }
    return resolved;
  }

  private void ensureAvailable(Product product, int requested) {
    if (product.getAmount() < requested) {
      throw new BadRequestApiException(
          "Insufficient stock for product '%s' (requested %d, available %d)"
              .formatted(product.getName(), requested, product.getAmount()));
    }
  }

  private BigDecimal itemTotal(ResolvedItem item) {
    return effectiveUnitPrice(item.product())
        .multiply(BigDecimal.valueOf(item.quantity()))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private OrderItem toOrderItem(Order order, ResolvedItem resolved) {
    Product product = resolved.product();
    return OrderItem.builder()
        .order(order)
        .product(product)
        .productName(product.getName())
        .quantity(resolved.quantity())
        .originalUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP))
        .priceAtPurchase(effectiveUnitPrice(product))
        .build();
  }

  private BigDecimal effectiveUnitPrice(Product product) {
    if (product.getDiscount() <= 0) {
      return product.getPrice().setScale(2, RoundingMode.HALF_UP);
    }
    BigDecimal multiplier = ONE_HUNDRED.subtract(BigDecimal.valueOf(product.getDiscount()));
    return product.getPrice().multiply(multiplier).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
  }

}
