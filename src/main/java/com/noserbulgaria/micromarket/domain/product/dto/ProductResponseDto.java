package com.noserbulgaria.micromarket.domain.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponseDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    BigDecimal price,

    int discount,

    boolean enabled,

    long amount

) {
}
