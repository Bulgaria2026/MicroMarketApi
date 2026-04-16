package com.noserbulgaria.micromarket.exception;

import java.util.UUID;

public final class ExceptionContexts {

  public static ExceptionContext of(String name) {
    return new ExceptionContext(name);
  }

  public static ExceptionContext fromUuid(UUID id) {
    return new ExceptionContext(id.toString());
  }

  public static ExceptionContext fromEmail(String email) {
    return new ExceptionContext(email);
  }
}
