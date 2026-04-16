package com.noserbulgaria.micromarket.exception;

public class BadRequestApiException extends AbstractApiRuntimeException {

  public BadRequestApiException(ExceptionContext context) {
    super("Bad request: '%s'.".formatted(context.name()));
  }
}
