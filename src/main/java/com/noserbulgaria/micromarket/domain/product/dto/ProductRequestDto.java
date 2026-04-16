package com.noserbulgaria.micromarket.domain.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequestDto(

    @NotBlank(message = "Name must not be blank")
    @Schema(example = "Test Product")
    String name,

    @NotBlank(message = "Description must not be blank")
    @Schema(example = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. ")
    String description,

    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than 0")
    @Schema(example = "199")
    BigDecimal price,

    @NotNull(message = "Discount must not be null")
    @Min(value = 0)
    @Max(value = 100)
    @Schema(example = "20")
    Integer discount,

    @NotNull(message = "Enabled status must not be null")
    @Schema(example = "false")
    Boolean enabled,

    @NotNull(message = "Amount must not be null")
    @PositiveOrZero(message = "Amount must be zero or greater")
    @Schema(example = "25")
    Long amount

) {
}
