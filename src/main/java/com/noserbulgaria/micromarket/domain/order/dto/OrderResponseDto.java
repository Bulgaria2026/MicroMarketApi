package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.order.OrderStatusType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OrderResponseDto(

    UUID id,

    OrderStatusType status,

    UUID customerId,

    Set<OrderItemResponseDto> orderItems,

    Instant createdAt,

    Instant updatedAt

) {
}
