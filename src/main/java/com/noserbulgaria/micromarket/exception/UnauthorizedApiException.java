package com.noserbulgaria.micromarket.exception;

import org.springframework.security.core.AuthenticationException;

public class UnauthorizedApiException extends AuthenticationException {

  public UnauthorizedApiException(ExceptionContext context) {
    super("Unauthorized: '%s'.".formatted(context.name()));
  }
}
