package com.noserbulgaria.micromarket.checkout;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code checkoutUrl} is the Stripe-hosted redirect the frontend sends the customer to. */
public record PlaceOrderResponse(

    UUID orderId,

    String orderNumber,

    BigDecimal subtotal,

    String checkoutUrl

) {
}
