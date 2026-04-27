package com.noserbulgaria.micromarket.mail.order;

import java.math.BigDecimal;
import java.util.List;

/**
 * View model for {@code email-templates/order-confirmation.html}.
 */
public record OrderConfirmationView(
    String number,
    String date,
    List<Item> items,
    BigDecimal subtotal,
    BigDecimal totalDiscount,
    BigDecimal total) {

  public record Item(
      String name,
      String description,
      int amount,
      BigDecimal price,
      int discount,
      BigDecimal lineTotal) {}
}
