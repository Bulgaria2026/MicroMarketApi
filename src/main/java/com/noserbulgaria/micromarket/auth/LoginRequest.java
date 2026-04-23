package com.noserbulgaria.micromarket.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "admin@micromarket.dev")
    String email,

    @NotBlank(message = "Password is required")
    @Schema(example = "admin123")
    String password

) {
}
