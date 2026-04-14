package com.noserbulgaria.micromarket.domain.product.dto;

import com.noserbulgaria.micromarket.generic.ExtendedDto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

public record ProductDto(

    UUID id,

    Instant createdAt,

    Instant updatedAt,

    @NotBlank(message = "Name must not be blank")
    String name,

    @NotBlank(message = "Description must not be blank")
    String description,

    @NotNull(message = "Price must not be null")
    @Positive(message = "Price must be greater than 0")
    Double price,

    @NotNull(message = "Discount must not be null")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    Double discount,

    @NotNull(message = "Enabled status must not be null")
    Boolean enabled,

    @NotNull(message = "Amount must not be null")
    @PositiveOrZero(message = "Amount must be zero or greater")
    Long amount

) implements ExtendedDto {}
