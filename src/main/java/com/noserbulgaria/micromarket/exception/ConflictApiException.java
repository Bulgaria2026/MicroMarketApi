package com.noserbulgaria.micromarket.exception;

public class ConflictApiException extends AbstractApiRuntimeException {

  public ConflictApiException(ExceptionContext context) {
    super("Conflict: '%s'.".formatted(context.name()));
  }
}
