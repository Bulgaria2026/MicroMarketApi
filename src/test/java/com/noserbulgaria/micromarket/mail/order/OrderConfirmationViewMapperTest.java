package com.noserbulgaria.micromarket.mail.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.noserbulgaria.micromarket.customer.Guest;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import com.noserbulgaria.micromarket.order.OrderStatusType;
import com.noserbulgaria.micromarket.product.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderConfirmationViewMapperTest {

  private final OrderConfirmationViewMapper mapper = new OrderConfirmationViewMapper();

  @Test
  void mapsWithoutDiscount() {
    Product product = product("Olive Oil 500ml", "Extra virgin, Puglia single-estate");
    OrderItem line = OrderItem.builder()
        .product(product)
        .productName(product.getName())
        .quantity(2)
        .originalUnitPrice(new BigDecimal("14.90"))
        .priceAtPurchase(new BigDecimal("14.90"))
        .build();
    Order order = order("ORD-100001", new BigDecimal("29.80"), Set.of(line));

    OrderConfirmationView view = mapper.toView(order);

    assertThat(view.number()).isEqualTo("ORD-100001");
    assertThat(view.items()).hasSize(1);
    OrderConfirmationView.Item item = view.items().getFirst();
    assertThat(item.name()).isEqualTo("Olive Oil 500ml");
    assertThat(item.description()).isEqualTo("Extra virgin, Puglia single-estate");
    assertThat(item.amount()).isEqualTo(2);
    assertThat(item.price()).isEqualByComparingTo("14.90");
    assertThat(item.discount()).isZero();
    assertThat(item.lineTotal()).isEqualByComparingTo("29.80");
    assertThat(view.subtotal()).isEqualByComparingTo("29.80");
    assertThat(view.totalDiscount()).isEqualByComparingTo("0.00");
    assertThat(view.total()).isEqualByComparingTo("29.80");
  }

  @Test
  void computesDiscountPercentAndAbsoluteDiscount() {
    Product product = product("Coffee Beans 1kg", "Single-origin Ethiopia");
    OrderItem line = OrderItem.builder()
        .product(product)
        .productName(product.getName())
        .quantity(2)
        .originalUnitPrice(new BigDecimal("100.00"))
        .priceAtPurchase(new BigDecimal("90.00"))
        .build();
    Order order = order("ORD-100002", new BigDecimal("180.00"), Set.of(line));

    OrderConfirmationView view = mapper.toView(order);

    OrderConfirmationView.Item item = view.items().getFirst();
    assertThat(item.discount()).isEqualTo(10);
    assertThat(item.lineTotal()).isEqualByComparingTo("180.00");
    assertThat(view.subtotal()).isEqualByComparingTo("200.00");
    assertThat(view.totalDiscount()).isEqualByComparingTo("20.00");
    assertThat(view.total()).isEqualByComparingTo("180.00");
  }

  @Test
  void handlesMultipleLinesWithMixedDiscounts() {
    OrderItem lineA = OrderItem.builder()
        .product(product("A", "alpha")).productName("A").quantity(3)
        .originalUnitPrice(new BigDecimal("10.00"))
        .priceAtPurchase(new BigDecimal("10.00"))
        .build();
    OrderItem lineB = OrderItem.builder()
        .product(product("B", "beta")).productName("B").quantity(1)
        .originalUnitPrice(new BigDecimal("20.00"))
        .priceAtPurchase(new BigDecimal("15.00"))
        .build();
    Order order = order("ORD-100003", new BigDecimal("45.00"), orderedSet(lineA, lineB));

    OrderConfirmationView view = mapper.toView(order);

    assertThat(view.items()).extracting(OrderConfirmationView.Item::discount).containsExactly(0, 25);
    assertThat(view.items()).extracting(OrderConfirmationView.Item::lineTotal)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactly(new BigDecimal("30.00"), new BigDecimal("15.00"));
    assertThat(view.subtotal()).isEqualByComparingTo("50.00");
    assertThat(view.totalDiscount()).isEqualByComparingTo("5.00");
    assertThat(view.total()).isEqualByComparingTo("45.00");
  }

  @Test
  void lineTotalReusesChargedPriceInsteadOfReconstructingFromRoundedPercent() {
    OrderItem line = OrderItem.builder()
        .product(product("Sourdough", "24h fermented")).productName("Sourdough").quantity(1)
        .originalUnitPrice(new BigDecimal("9.90"))
        .priceAtPurchase(new BigDecimal("8.90"))
        .build();
    Order order = order("ORD-100004", new BigDecimal("8.90"), Set.of(line));

    OrderConfirmationView view = mapper.toView(order);

    OrderConfirmationView.Item item = view.items().getFirst();
    assertThat(item.discount()).isEqualTo(10);
    assertThat(item.lineTotal()).isEqualByComparingTo("8.90");
    assertThat(view.total()).isEqualByComparingTo("8.90");
  }

  private static Product product(String name, String description) {
    Product product = new Product();
    product.setName(name);
    product.setDescription(description);
    product.setPrice(new BigDecimal("1.00"));
    product.setEnabled(true);
    return product;
  }

  private static Order order(String number, BigDecimal total, Set<OrderItem> items) {
    Guest customer = new Guest();
    customer.setEmail("buyer@example.com");
    Order order = Order.builder()
        .orderNumber(number)
        .status(OrderStatusType.PAID)
        .customer(customer)
        .email("buyer@example.com")
        .totalAmount(total)
        .orderItems(new LinkedHashSet<>(items))
        .build();
    order.setCreatedAt(Instant.parse("2026-04-23T10:15:30Z"));
    return order;
  }

  private static Set<OrderItem> orderedSet(OrderItem... items) {
    return new LinkedHashSet<>(List.of(items));
  }
}
