package com.noserbulgaria.micromarket.generic;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic dto interface to force the implementation of the common fields for all DTOs in the application.
 * Implementations of this interface will have to add the same annotations as used in the ExtendedDto to ensure
 * effectivity.
 */
public interface ExtendedDto {

  @NotNull
  UUID id();

  @NotNull
  Instant createdAt();

  Instant updatedAt();
}
