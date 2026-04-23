package com.noserbulgaria.micromarket.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemResponse(

    UUID id,

    UUID productId,

    String productName,

    Integer quantity,

    BigDecimal originalUnitPrice,

    BigDecimal priceAtPurchase,

    Instant createdAt,

    Instant updatedAt

) {
}
