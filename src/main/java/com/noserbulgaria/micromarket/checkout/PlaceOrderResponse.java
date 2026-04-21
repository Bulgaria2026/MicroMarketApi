package com.noserbulgaria.micromarket.checkout;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param orderId      internal UUID of the new order
 * @param orderNumber  human-readable code ({@code MM-NNNNNN}) shown on receipts and used by support
 * @param totalAmount  amount charged in EUR
 * @param clientSecret short-lived token the storefront passes to Stripe Elements to confirm the payment client-side
 */
public record PlaceOrderResponse(

    UUID orderId,

    String orderNumber,

    BigDecimal totalAmount,

    String clientSecret

) {
}
