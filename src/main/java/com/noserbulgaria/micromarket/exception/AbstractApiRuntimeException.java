package com.noserbulgaria.micromarket.exception;

public abstract class AbstractApiRuntimeException extends RuntimeException {

  protected AbstractApiRuntimeException(String detail) {
    super(detail);
  }
}
