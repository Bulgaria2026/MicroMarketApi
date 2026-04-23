package com.noserbulgaria.micromarket.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductHistoryResponse(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    BigDecimal price,

    int discount,

    boolean enabled,

    long amount,

    long revisionNumber,

    Instant revisionTimestamp,

    String revisionType

) {
}
