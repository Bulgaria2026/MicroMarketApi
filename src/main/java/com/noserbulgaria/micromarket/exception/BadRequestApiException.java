package com.noserbulgaria.micromarket.exception;


public class BadRequestApiException extends AbstractApiRuntimeException {

  public BadRequestApiException(String message) {
    super(message);
  }
}
