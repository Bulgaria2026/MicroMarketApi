package com.noserbulgaria.micromarket.exception;

public class InternalServerApiException extends AbstractApiRuntimeException {

  public InternalServerApiException(ExceptionContext context) {
    super("Internal server error: '%s'.".formatted(context.name()));
  }
}
