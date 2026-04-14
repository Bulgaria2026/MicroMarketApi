package com.noserbulgaria.micromarket.domain.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order item details")
public record OrderItemRequestDTO(
    @Schema(description = "Unique identifier of the order item") UUID id,
    @Schema(description = "Quantity of the product ordered") Integer quantity,
    @Schema(description = "Price of the product at the time of purchase") BigDecimal priceAtPurchase,
    @Schema(description = "Timestamp when the order item was created") Instant createdAt,
    @Schema(description = "Timestamp when the order item was last updated") Instant updatedAt
) {
}
