package com.noserbulgaria.micromarket.domain.product;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record ProductFilter(
    @Nullable String name,
    @Nullable BigDecimal minPrice,
    @Nullable BigDecimal maxPrice,
    @Nullable Boolean enabled
) {
}
