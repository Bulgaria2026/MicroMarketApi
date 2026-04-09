package com.noserbulgaria.micromarket.domain.stock.dto;

import com.noserbulgaria.micromarket.domain.product.dto.ProductDto;
import com.noserbulgaria.micromarket.generic.ExtendedDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;

import java.time.Instant;
import java.util.UUID;

public record StockDto(

    @NotNull
    UUID id,

    @NotNull
    Instant createdAt,

    Instant updatedAt,

    @NotNull(message = "Products must not be null")
    Set<ProductDto> products,

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be greater than 0")
    Long amount

) implements ExtendedDto {}


