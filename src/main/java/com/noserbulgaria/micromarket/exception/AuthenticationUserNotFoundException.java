package com.noserbulgaria.micromarket.exception;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public final class AuthenticationUserNotFoundException extends UsernameNotFoundException {

  public AuthenticationUserNotFoundException(String message) {
    super(message);
  }
}
