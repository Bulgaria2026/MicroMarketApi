package com.noserbulgaria.micromarket.domain.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for Order.
 */
@Schema(description = "Order details")
public record OrderDto(
    @Schema(description = "Unique identifier of the order")
    UUID id,
    @Schema(description = "Current status of the order")
    OrderStatusType status,
    @Schema(description = "ID of the customer who placed the order")
    UUID customerId,
    @Schema(description = "List of items in the order")
    Set<OrderItemDto> orderItems,
    @Schema(description = "Timestamp when the order was created")
    Instant createdAt,
    @Schema(description = "Timestamp when the order was last updated")
    Instant updatedAt
) {}
