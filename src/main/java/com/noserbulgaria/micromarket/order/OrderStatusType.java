package com.noserbulgaria.micromarket.order;

public enum OrderStatusType {
  PENDING_PAYMENT,
  PAID,
  PAYMENT_FAILED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatusType next) {
    return switch (this) {
      case PENDING_PAYMENT -> next == PAID || next == PAYMENT_FAILED || next == CANCELLED;
      case PAID, PAYMENT_FAILED, CANCELLED -> false;
    };
  }
}
