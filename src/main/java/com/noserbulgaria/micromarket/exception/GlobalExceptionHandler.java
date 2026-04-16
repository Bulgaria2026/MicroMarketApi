package com.noserbulgaria.micromarket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String ABOUT_BLANK = "about:blank";

  @ExceptionHandler(NotFoundApiException.class)
  public ProblemDetail handleNotFound(NotFoundApiException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Not Found");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(BadRequestApiException.class)
  public ProblemDetail handleBadRequest(BadRequestApiException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Bad Request");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(ConflictApiException.class)
  public ProblemDetail handleConflict(ConflictApiException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Conflict");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(UnauthorizedApiException.class)
  public ProblemDetail handleUnauthorized(UnauthorizedApiException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Unauthorized");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(ForbiddenApiException.class)
  public ProblemDetail handleForbidden(ForbiddenApiException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Forbidden");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(AuthenticationUserNotFoundException.class)
  public ProblemDetail handleAuthenticationUserNotFound(AuthenticationUserNotFoundException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Unauthorized");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Request body validation failed."
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Request Validation Failed");
    problemDetail.setInstance(URI.create(requestPath(request)));
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .toList();
    problemDetail.setProperty("errors", errors);
    return problemDetail;
  }

  @ExceptionHandler(
      {
          MethodArgumentTypeMismatchException.class,
          MissingServletRequestParameterException.class
      }
  )
  public ProblemDetail handleRequestBindingException(Exception ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Request Binding Failed");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        "Resource '%s' was not found.".formatted(ex.getResourcePath())
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Resource Not Found");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "Access to '%s' is forbidden.".formatted(requestPath(request))
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Access Denied");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Authentication for '%s' failed.".formatted(requestPath(request))
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Authentication Failed");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Authentication is required to access '%s'.".formatted(requestPath(request))
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Unauthorized");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGlobalException(Exception ex, WebRequest request) {
    log.error("Unexpected error", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred for '%s'.".formatted(requestPath(request))
    );
    problemDetail.setType(URI.create(ABOUT_BLANK));
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(URI.create(requestPath(request)));
    return problemDetail;
  }

  private String requestPath(WebRequest request) {
    return request.getDescription(false).replace("uri=", "");
  }
}
