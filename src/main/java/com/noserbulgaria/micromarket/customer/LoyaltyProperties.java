package com.noserbulgaria.micromarket.customer;

import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "loyalty")
public record LoyaltyProperties(
    @DefaultValue("1")
    @DecimalMin("0.0")
    BigDecimal pointsPerEuro
) {
}
