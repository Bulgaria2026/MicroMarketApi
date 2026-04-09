package com.noserbulgaria.micromarket.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for Order Product.
 */
@Schema(description = "Order product details")
public record OrderProductDto(
                              @Schema(description = "Unique identifier of the order product") UUID id,
                              @Schema(description = "Quantity of the product ordered") Integer quantity,
                              @Schema(description = "Price of the product at the time of purchase") BigDecimal priceAtPurchase,
                              @Schema(description = "Timestamp when the order product was created") Instant createdAt,
                              @Schema(description = "Timestamp when the order product was last updated") Instant updatedAt
) {
}
