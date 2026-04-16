package com.noserbulgaria.micromarket.domain.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductHistoryDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    BigDecimal price,

    Integer discount,

    Boolean enabled,

    Long amount,

    Long revisionNumber,

    Instant revisionTimestamp,

    String revisionType

) {
}
