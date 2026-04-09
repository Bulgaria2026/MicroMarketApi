package com.noserbulgaria.micromarket.security.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a registration attempt uses an email already associated with an existing account.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateEmailException extends RuntimeException {

  public DuplicateEmailException(String email) {
    super("Email already registered: " + email);
  }
}
