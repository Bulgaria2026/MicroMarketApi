package com.noserbulgaria.micromarket.domain.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductHistoryDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    String name,

    String description,

    Double price,

    Double discount,

    Boolean enabled,

    Long amount,

    Long revisionNumber,

    Instant revisionTimestamp,

    String revisionType

) {
}
