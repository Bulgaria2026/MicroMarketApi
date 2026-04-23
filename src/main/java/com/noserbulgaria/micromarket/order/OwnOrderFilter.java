package com.noserbulgaria.micromarket.order;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record OwnOrderFilter(
    @Nullable Instant fromDate,
    @Nullable Instant toDate
) {
}
