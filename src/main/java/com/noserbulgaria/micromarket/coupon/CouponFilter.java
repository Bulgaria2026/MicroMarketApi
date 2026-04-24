package com.noserbulgaria.micromarket.coupon;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record CouponFilter(
    @Nullable UUID userId,
    @Nullable Boolean active
) {
}
