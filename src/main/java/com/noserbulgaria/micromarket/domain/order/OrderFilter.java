package com.noserbulgaria.micromarket.domain.order;

import java.time.Instant;
import java.util.UUID;

public record OrderFilter(
    Instant fromDate, Instant toDate, UUID customerId, OrderStatusType status
) {
}
