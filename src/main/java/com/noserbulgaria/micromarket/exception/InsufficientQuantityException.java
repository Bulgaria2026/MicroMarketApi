package com.noserbulgaria.micromarket.exception;

/**
 * Exception thrown when there is insufficient quantity in stock.
 */
public class InsufficientQuantityException extends RuntimeException {

  public InsufficientQuantityException(String message) {
    super(message);
  }

  public InsufficientQuantityException(String message, Throwable cause) {
    super(message, cause);
  }
}

