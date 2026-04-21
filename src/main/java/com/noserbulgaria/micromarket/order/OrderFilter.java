package com.noserbulgaria.micromarket.order;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record OrderFilter(
    @Nullable Instant fromDate,
    @Nullable Instant toDate,
    @Nullable UUID customerId,
    @Nullable OrderStatusType status
) {
}
