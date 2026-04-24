package com.noserbulgaria.micromarket.couponoffer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponOfferRequest(

    @NotBlank
    @Size(max = 80, message = "Name must be at most 80 characters")
    @Schema(example = "Coffee discount")
    String name,

    @Nullable
    @Size(max = 500, message = "Description must be at most 500 characters")
    @Schema(example = "Spend points to get 5 EUR off your next order")
    String description,

    @Nullable
    @Schema(example = "2026-04-23T09:00:00Z")
    Instant startDate,

    @Nullable
    @Schema(example = "2026-12-31T23:59:59Z")
    Instant expiryDate,

    @Min(value = 0, message = "Point cost must be at least 0")
    @Schema(example = "100")
    int pointCost,

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount off must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount off must have up to 2 decimal places")
    @Schema(example = "5.00")
    BigDecimal amountOff,

    @Nullable
    @Positive(message = "Max purchases must be greater than 0")
    @Schema(example = "25")
    Integer maxPurchases,

    @Nullable
    @Schema(example = "true")
    Boolean active
) {
}
