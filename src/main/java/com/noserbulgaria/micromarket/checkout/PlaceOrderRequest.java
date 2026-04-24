package com.noserbulgaria.micromarket.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(

    @NotEmpty @Valid List<PlaceOrderItem> items,

    @Email @Nullable String email,

    @Nullable UUID couponId
) {
}
