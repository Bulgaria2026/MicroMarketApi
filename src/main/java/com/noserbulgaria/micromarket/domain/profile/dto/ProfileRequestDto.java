package com.noserbulgaria.micromarket.domain.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProfileRequestDto(

    @NotNull(message = "Points must not be null")
    @Min(value = 0, message = "Points must be zero or greater")
    @Schema(example = "150")
    Long points

) {
}
