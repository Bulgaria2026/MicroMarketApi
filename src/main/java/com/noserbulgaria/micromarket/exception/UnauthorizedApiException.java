package com.noserbulgaria.micromarket.exception;

import org.springframework.security.core.AuthenticationException;

public class UnauthorizedApiException extends AuthenticationException {

  public UnauthorizedApiException(String message) {
    super(message);
  }
}
