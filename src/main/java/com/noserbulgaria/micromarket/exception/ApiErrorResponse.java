package com.noserbulgaria.micromarket.exception;

import java.time.Instant;

/**
 * Standard error response DTO for API error responses.
 */
public record ApiErrorResponse (
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
