package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerResolver;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.order.*;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentIntent;
import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import com.noserbulgaria.micromarket.product.Product;
import com.noserbulgaria.micromarket.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Orchestrates the {@code POST /order} use case across the Order, Customer and Stripe boundaries: validates the
 * requested items, resolves (or creates) the Customer, persists the Order in {@code PENDING_PAYMENT}, asks Stripe to
 * create a PaymentIntent, and stitches the intent id back onto the order. Asynchronous follow-up on the webhook lives
 * in {@link OrderSettlementService}.
 */
@Service
@RequiredArgsConstructor
public class OrderPlacementService {

  private static final String CURRENCY = "EUR";
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;
  private final ProfileRepository profileRepository;
  private final CustomerResolver customerResolver;
  private final StripePaymentProvider stripePaymentProvider;
  private final OrderNumberGenerator orderNumberGenerator;

  /**
   * A requested item after its product has been loaded and validated. Quantities for duplicate product ids in the
   * incoming request are already merged.
   */
  private record ResolvedItem(Product product, int quantity) {
  }

  @Transactional
  public PlaceOrderResponse placeOrder(PlaceOrderRequest request, @Nullable CustomUserDetails userDetails) {
    List<ResolvedItem> items = resolveItems(request.items());

    Customer customer = customerResolver.resolveForCheckout(userDetails, request.email());
    String email = resolveEmail(userDetails, request);
    String stripeCustomerId = ensureStripeCustomerId(customer, email);

    BigDecimal total = items.stream()
        .map(this::itemTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    Order order = Order.builder()
        .orderNumber(orderNumberGenerator.next())
        .status(OrderStatusType.PENDING_PAYMENT)
        .customer(customer)
        .email(email)
        .totalAmount(total)
        .build();
    for (ResolvedItem item : items) {
      order.getOrderItems().add(toOrderItem(order, item));
    }
    order = orderRepository.saveAndFlush(order);

    StripePaymentIntent intent = stripePaymentProvider.initiate(order.getId(), total, CURRENCY, stripeCustomerId);
    order.setStripePaymentIntentId(intent.id());
    order = orderRepository.saveAndFlush(order);

    return new PlaceOrderResponse(order.getId(), order.getOrderNumber(), total, intent.clientSecret());
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

  private @Nullable String ensureStripeCustomerId(Customer customer, String email) {
    if (!(customer instanceof Profile profile)) {
      return null;
    }
    if (profile.getStripeCustomerId() != null) {
      return profile.getStripeCustomerId();
    }
    String stripeCustomerId = stripePaymentProvider.createCustomer(email);
    profile.setStripeCustomerId(stripeCustomerId);
    profileRepository.saveAndFlush(profile);
    return stripeCustomerId;
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
