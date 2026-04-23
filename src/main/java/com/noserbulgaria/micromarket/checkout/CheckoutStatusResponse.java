package com.noserbulgaria.micromarket.checkout;

import com.noserbulgaria.micromarket.order.OrderStatusType;

/** Minimal view for the post-redirect poll — session id is low-trust, don't leak line items or email through it. */
public record CheckoutStatusResponse(String orderNumber, OrderStatusType status) {
}
