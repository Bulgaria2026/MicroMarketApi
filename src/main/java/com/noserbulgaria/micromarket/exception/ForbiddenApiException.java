package com.noserbulgaria.micromarket.exception;

import org.springframework.security.access.AccessDeniedException;

public class ForbiddenApiException extends AccessDeniedException {

  public ForbiddenApiException(ExceptionContext context) {
    super("Forbidden: '%s'.".formatted(context.name()));
  }
}
