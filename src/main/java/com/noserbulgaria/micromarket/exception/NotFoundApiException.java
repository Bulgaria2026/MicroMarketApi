package com.noserbulgaria.micromarket.exception;

public class NotFoundApiException extends AbstractApiRuntimeException {

  public NotFoundApiException(ExceptionContext context) {
    super("Resource not found: '%s'.".formatted(context.name()));
  }
}
