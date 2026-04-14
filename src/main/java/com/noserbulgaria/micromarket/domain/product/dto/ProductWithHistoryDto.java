package com.noserbulgaria.micromarket.domain.product.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductWithHistoryDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    Double price,

    Double discount,

    Boolean enabled,

    Long amount,

    List<ProductHistoryDto> history

) {
}
