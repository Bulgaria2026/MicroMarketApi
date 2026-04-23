package com.noserbulgaria.micromarket.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponRequest(

    @Nullable
    @Schema(example = "a0000000-0000-0000-0000-000000000001")
    UUID userId,

    @Nullable
    @Size(max = 500, message = "Code must be at most 500 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9-]+$",
        message = "Code may only contain letters, digits, and hyphens"
    )
    @Schema(example = "WELCOME-10")
    String code,

    @Nullable
    @Size(max = 40, message = "Name must be at most 40 characters")
    @Schema(example = "Welcome coupon")
    String name,

    @Nullable
    @Schema(example = "2026-04-23T09:00:00Z")
    Instant startDate,

    @Nullable
    @Schema(example = "2026-12-31T23:59:59Z")
    Instant expiryDate,

    @Min(value = 0, message = "Point cost must be at least 0")
    @Schema(example = "0")
    int pointCost,

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount off must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount off must have up to 2 decimal places")
    @Schema(example = "5.00")
    BigDecimal amountOff,

    @Nullable
    @Positive(message = "Max redemptions must be greater than 0")
    @Schema(example = "10")
    Integer maxRedemptions,

    @Nullable
    @Schema(example = "true")
    Boolean active
) {
}
