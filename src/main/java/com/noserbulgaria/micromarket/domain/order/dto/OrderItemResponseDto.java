package com.noserbulgaria.micromarket.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemResponseDto(

    UUID id,

    UUID productId,

    Integer quantity,

    BigDecimal priceAtPurchase,

    Instant createdAt,

    Instant updatedAt

) {
}
