package com.noserbulgaria.micromarket.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaceOrderItem(

    @NotNull UUID productId,

    @Min(1) int quantity
) {
}
