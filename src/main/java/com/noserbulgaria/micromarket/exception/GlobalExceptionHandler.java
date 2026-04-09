package com.noserbulgaria.micromarket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Provides consistent error responses across all endpoints.
 * Define exceptions here to avoid internal server error (500) responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
      EntityNotFoundException ex,
      WebRequest request) {
    log.warn("Entity not found: {}", ex.getMessage());

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        ex.getMessage(),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(InsufficientQuantityException.class)
  public ResponseEntity<ApiErrorResponse> handleInsufficientQuantity(
      InsufficientQuantityException ex,
      WebRequest request) {
    log.warn("Insufficient quantity: {}", ex.getMessage());

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        ex.getMessage(),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex,
      WebRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        422,
        "Validation Failed",
        message,
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatusCode.valueOf(422));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(
      BadCredentialsException ex,
      WebRequest request) {
    log.warn("Bad credentials: {}", ex.getMessage());

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        ex.getMessage(),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGlobalException(
      Exception ex,
      WebRequest request) {
    log.error("Unexpected error", ex);

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        "An unexpected error occurred",
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
