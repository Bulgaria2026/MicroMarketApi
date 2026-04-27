package com.noserbulgaria.micromarket.mail.order;

import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
class OrderConfirmationViewMapper {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  OrderConfirmationView toView(Order order) {
    List<OrderConfirmationView.Item> items = order.getOrderItems().stream()
        .map(OrderConfirmationViewMapper::toItem)
        .toList();

    BigDecimal subtotal = order.getSubtotal();
    BigDecimal paidTotal = Objects.requireNonNull(
        order.getPaidTotal(),
        "paidTotal is required for an order confirmation email"
    );
    BigDecimal totalDiscount = subtotal.subtract(paidTotal).max(BigDecimal.ZERO);

    String date = DATE.format(order.getCreatedAt().atZone(ZoneId.systemDefault()));

    return new OrderConfirmationView(
        order.getOrderNumber(),
        date,
        items,
        subtotal,
        totalDiscount,
        paidTotal);
  }

  private static OrderConfirmationView.Item toItem(OrderItem item) {
    BigDecimal original = item.getOriginalUnitPrice();
    BigDecimal charged = item.getPriceAtPurchase();
    int discountPct = original.signum() == 0
        ? 0
        : original.subtract(charged)
            .multiply(BigDecimal.valueOf(100))
            .divide(original, 0, RoundingMode.HALF_UP)
            .intValueExact();

    BigDecimal lineTotal = charged.multiply(BigDecimal.valueOf(item.getQuantity()));

    return new OrderConfirmationView.Item(
        item.getProductName(),
        item.getProduct().getDescription(),
        item.getQuantity(),
        original,
        Math.max(discountPct, 0),
        lineTotal);
  }
}
