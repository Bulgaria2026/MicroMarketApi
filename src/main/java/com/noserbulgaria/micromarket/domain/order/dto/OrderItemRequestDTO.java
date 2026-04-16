package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.generic.ExtendedDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemRequestDTO(

    UUID id,

    UUID productId,

    Integer quantity,

    BigDecimal priceAtPurchase,

    Instant createdAt,

    Instant updatedAt

) implements ExtendedDto {
}
