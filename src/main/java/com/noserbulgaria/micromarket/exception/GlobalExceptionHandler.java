package com.noserbulgaria.micromarket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API. Provides consistent error responses across all endpoints. Define exceptions
 * here to avoid internal server error (500) responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private String messageOrFallback(Exception ex, String fallback) {
    return Objects.requireNonNullElse(ex.getMessage(), fallback);
  }

  @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
      Exception ex,
      WebRequest request
  ) {
    log.warn("Entity not found: {}", ex.getMessage());

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        messageOrFallback(ex, "Resource not found"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  //region BadRequestException
  // separated for readability and less generic response messages
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex,
      WebRequest request
  ) {
    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Validation Failed",
        message,
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequestException(
      BadRequestException ex,
      WebRequest request
  ) {
    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        messageOrFallback(ex, "Bad request"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({
      MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiErrorResponse> handleRequestBindingException(
      Exception ex,
      WebRequest request
  ) {
    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Bad Request",
        messageOrFallback(ex, "Bad request"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }
  //endregion

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      AccessDeniedException ex,
      WebRequest request
  ) {
    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        messageOrFallback(ex, "Access denied"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(
      BadCredentialsException ex,
      WebRequest request
  ) {
    log.warn("Bad credentials: {}", ex.getMessage());

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        messageOrFallback(ex, "Invalid credentials"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthorized(
      UnauthorizedException ex,
      WebRequest request
  ) {
    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        messageOrFallback(ex, "Unauthorized"),
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
      ResponseStatusException ex,
      WebRequest request
  ) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();

    ApiErrorResponse error = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getDescription(false).replace("uri=", "")
    );

    return new ResponseEntity<>(error, status);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGlobalException(
      Exception ex,
      WebRequest request
  ) {
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
