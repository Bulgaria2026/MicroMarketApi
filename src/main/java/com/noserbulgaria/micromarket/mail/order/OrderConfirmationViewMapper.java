package com.noserbulgaria.micromarket.mail.order;

import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class OrderConfirmationViewMapper {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  OrderConfirmationView toView(Order order) {
    List<OrderConfirmationView.Item> items = order.getOrderItems().stream()
        .map(OrderConfirmationViewMapper::toItem)
        .toList();

    BigDecimal subtotal = items.stream()
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.amount())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalDiscount = subtotal.subtract(order.getTotalAmount()).max(BigDecimal.ZERO);

    String date = DATE.format(order.getCreatedAt().atZone(ZoneId.systemDefault()));

    return new OrderConfirmationView(
        order.getOrderNumber(),
        date,
        items,
        subtotal,
        totalDiscount,
        order.getTotalAmount());
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

    return new OrderConfirmationView.Item(
        item.getProductName(),
        item.getProduct().getDescription(),
        item.getQuantity(),
        original,
        Math.max(discountPct, 0));
  }
}
