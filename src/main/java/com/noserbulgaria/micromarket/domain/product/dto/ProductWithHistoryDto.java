package com.noserbulgaria.micromarket.domain.product.dto;

import com.noserbulgaria.micromarket.generic.ExtendedDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductWithHistoryDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    BigDecimal price,

    Integer discount,

    Boolean enabled,

    Long amount,

    List<ProductHistoryDto> history

) implements ExtendedDto {
}
