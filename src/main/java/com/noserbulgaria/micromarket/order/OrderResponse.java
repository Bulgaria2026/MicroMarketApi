package com.noserbulgaria.micromarket.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record OrderResponse(

    UUID id,

    String orderNumber,

    OrderStatusType status,

    UUID customerId,

    String email,

    @Nullable String stripeCheckoutSessionId,

    BigDecimal subtotal,

    @Nullable BigDecimal paidTotal,

    Set<OrderItemResponse> orderItems,

    Instant createdAt,

    Instant updatedAt

) {
}
