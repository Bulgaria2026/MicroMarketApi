package com.noserbulgaria.micromarket.order;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record OrderFilter(
    @Nullable Instant fromDate,
    @Nullable Instant toDate,
    @Nullable String orderNumber,
    @Nullable String email,
    @Nullable OrderStatusType status
) {
}
