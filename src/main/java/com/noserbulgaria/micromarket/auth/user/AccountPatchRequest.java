package com.noserbulgaria.micromarket.auth.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import org.jspecify.annotations.Nullable;

public record AccountPatchRequest(

    @Nullable
    @Email(message = "A valid email must be provided")
    @Schema(example = "admin@micromarket.dev")
    String email,

    @Nullable
    @Schema(example = "ADMINISTRATOR")
    Role role,

    @Nullable
    @Schema(example = "ACTIVE")
    AccountStatus status

) {

  @AssertTrue(message = "At least one field must be provided")
  public boolean hasUpdates() {
    return email != null || role != null || status != null;
  }
}
