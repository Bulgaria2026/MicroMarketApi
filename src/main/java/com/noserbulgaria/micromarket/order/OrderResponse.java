package com.noserbulgaria.micromarket.order;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OrderResponse(

    UUID id,

    String orderNumber,

    OrderStatusType status,

    UUID customerId,

    String email,

    @Nullable UUID couponId,

    @Nullable String couponCode,

    @Nullable BigDecimal couponAmountOff,

    @Nullable String stripeCheckoutSessionId,

    BigDecimal totalAmount,

    Set<OrderItemResponse> orderItems,

    Instant createdAt,

    Instant updatedAt

) {
}
