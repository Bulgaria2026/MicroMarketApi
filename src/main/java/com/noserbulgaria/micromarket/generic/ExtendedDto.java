package com.noserbulgaria.micromarket.generic;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic dto interface to force the implementation of the common fields for all DTOs in the application.
 * Implement this interface in all DTOs that require the common fields (id, createdAt, updatedAt).
 */
public interface ExtendedDto {

  UUID id();

  Instant createdAt();

  Instant updatedAt();
}
