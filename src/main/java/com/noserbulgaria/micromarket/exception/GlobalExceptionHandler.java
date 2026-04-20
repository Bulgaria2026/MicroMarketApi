package com.noserbulgaria.micromarket.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Pattern ENUM_VALUES_PATTERN = Pattern.compile("Enum class: \\[(.*)]");
  private static final Pattern ENUM_TYPE_PATTERN = Pattern.compile("type `([^`]+)`");

  // --- Custom API exception handlers ---

  @ExceptionHandler(NotFoundApiException.class)
  public ProblemDetail handleNotFound(NotFoundApiException ex, WebRequest request) {
    return buildProblem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
  }

  @ExceptionHandler(BadRequestApiException.class)
  public ProblemDetail handleBadRequest(BadRequestApiException ex, WebRequest request) {
    return buildProblem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
  }

  @ExceptionHandler(ConflictApiException.class)
  public ProblemDetail handleConflict(ConflictApiException ex, WebRequest request) {
    return buildProblem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
  }

  @ExceptionHandler(UnauthorizedApiException.class)
  public ProblemDetail handleUnauthorized(UnauthorizedApiException ex, WebRequest request) {
    return buildProblem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
  }

  @ExceptionHandler(ForbiddenApiException.class)
  public ProblemDetail handleForbidden(ForbiddenApiException ex, WebRequest request) {
    return buildProblem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
  }

  @ExceptionHandler(AuthenticationUserNotFoundException.class)
  public ProblemDetail handleAuthenticationUserNotFound(AuthenticationUserNotFoundException ex, WebRequest request) {
    return buildProblem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
  }

  // --- Spring Security exception handlers ---

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    return buildProblem(HttpStatus.FORBIDDEN, "Access Denied",
        "Access to '%s' is forbidden.".formatted(requestPath(request)), request);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex, WebRequest request) {
    return buildProblem(HttpStatus.UNAUTHORIZED, "Authentication Failed",
        "Authentication for '%s' failed.".formatted(requestPath(request)), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
    return buildProblem(HttpStatus.UNAUTHORIZED, "Unauthorized",
        "Authentication is required to access '%s'.".formatted(requestPath(request)), request);
  }

  // --- Spring MVC exception overrides ---

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "Request Validation Failed",
        "Request body validation failed.", request);
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .toList();
    pd.setProperty("errors", errors);
    return handleExceptionInternal(ex, pd, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "Request Binding Failed",
        ex.getMessage(), request);
    return handleExceptionInternal(ex, pd, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "Request Binding Failed",
        ex.getMessage(), request);
    return handleExceptionInternal(ex, pd, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail pd = buildProblem(HttpStatus.BAD_REQUEST, "Request Binding Failed",
        resolveMessageNotReadableDetail(ex), request);
    return handleExceptionInternal(ex, pd, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail pd = buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found",
        "Resource '%s' was not found.".formatted(ex.getResourcePath()), request);
    return handleExceptionInternal(ex, pd, headers, status, request);
  }

  // --- Global catch-all ---

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGlobalException(Exception ex, WebRequest request) {
    log.error("Unexpected error", ex);
    return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
        "An unexpected error occurred for '%s'.".formatted(requestPath(request)), request);
  }

  // --- Helpers ---

  private ProblemDetail buildProblem(HttpStatus status, String title, @Nullable String detail, WebRequest request) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    pd.setInstance(URI.create(requestPath(request)));
    return pd;
  }

  private String resolveMessageNotReadableDetail(HttpMessageNotReadableException ex) {
    InvalidFormatException invalidFormatException = findCause(ex, InvalidFormatException.class);
    if (invalidFormatException != null && invalidFormatException.getTargetType().isEnum()) {
      String field = invalidFormatException.getPath().stream()
          .map(JsonMappingException.Reference::getFieldName)
          .filter(name -> name != null && !name.isBlank())
          .reduce((_, second) -> second)
          .orElse("value");
      String allowedValues = Arrays.stream(invalidFormatException.getTargetType().getEnumConstants())
          .map(String::valueOf)
          .sorted()
          .reduce((left, right) -> left + ", " + right)
          .orElse("");
      return "Invalid value for '%s'. Allowed values: [%s]".formatted(field, allowedValues);
    }

    String message = ex.getMessage();
    if (message != null) {
      Matcher matcher = ENUM_VALUES_PATTERN.matcher(message);
      if (message.contains("Enum class") && matcher.find()) {
        String field = resolveEnumFieldName(message);
        String allowedValues = Arrays.stream(matcher.group(1).split(","))
            .map(String::trim)
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
        return "Invalid value for '%s'. Allowed values: [%s]".formatted(field, allowedValues);
      }
    }

    return message != null ? message : "Malformed JSON request body";
  }

  private <T extends Throwable> @Nullable T findCause(Throwable throwable, Class<T> type) {
    Throwable current = throwable;
    while (current != null) {
      if (type.isInstance(current)) {
        return type.cast(current);
      }
      current = current.getCause();
    }
    return null;
  }

  private String resolveEnumFieldName(String message) {
    Matcher matcher = ENUM_TYPE_PATTERN.matcher(message);
    if (!matcher.find()) {
      return "value";
    }

    return switch (matcher.group(1)) {
      case "com.noserbulgaria.micromarket.security.user.Role" -> "role";
      case "com.noserbulgaria.micromarket.security.user.AccountStatus" -> "status";
      default -> "value";
    };
  }

  private String requestPath(WebRequest request) {
    return request.getDescription(false).replace("uri=", "");
  }
}
