package com.noserbulgaria.micromarket.domain.order.dto;

import com.noserbulgaria.micromarket.domain.product.dto.ProductResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderItemDetailResponseDto(

    UUID id,

    ProductResponseDto product,

    Integer quantity,

    BigDecimal priceAtPurchase,

    Instant createdAt,

    Instant updatedAt

) {
}
