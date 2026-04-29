package com.noserbulgaria.micromarket.exception;

import com.stripe.exception.StripeException;
import org.jspecify.annotations.Nullable;

public class StripeApiException extends RuntimeException {

  private final boolean invalidRequestMeansNotFound;

  public StripeApiException(String message) {
    super(message);
    this.invalidRequestMeansNotFound = false;
  }

  public StripeApiException(String message, StripeException cause) {
    this(message, cause, false);
  }

  public StripeApiException(String message, StripeException cause, boolean invalidRequestMeansNotFound) {
    super(message, cause);
    this.invalidRequestMeansNotFound = invalidRequestMeansNotFound;
  }

  @Override
  public @Nullable StripeException getCause() {
    return (StripeException) super.getCause();
  }

  public boolean invalidRequestMeansNotFound() {
    return invalidRequestMeansNotFound;
  }
}
