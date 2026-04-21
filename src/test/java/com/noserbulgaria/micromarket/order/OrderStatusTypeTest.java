package com.noserbulgaria.micromarket.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTypeTest {

  @Test
  void pendingPayment_canTransitionTo_paidFailedCancelled() {
    assertThat(OrderStatusType.PENDING_PAYMENT.canTransitionTo(OrderStatusType.PAID)).isTrue();
    assertThat(OrderStatusType.PENDING_PAYMENT.canTransitionTo(OrderStatusType.PAYMENT_FAILED)).isTrue();
    assertThat(OrderStatusType.PENDING_PAYMENT.canTransitionTo(OrderStatusType.CANCELLED)).isTrue();
    assertThat(OrderStatusType.PENDING_PAYMENT.canTransitionTo(OrderStatusType.PENDING_PAYMENT)).isFalse();
  }

  @Test
  void terminalStates_cannotTransition() {
    for (OrderStatusType terminal : new OrderStatusType[]{
        OrderStatusType.PAID, OrderStatusType.PAYMENT_FAILED, OrderStatusType.CANCELLED
    }) {
      for (OrderStatusType next : OrderStatusType.values()) {
        assertThat(terminal.canTransitionTo(next))
            .as("%s -> %s must not be allowed", terminal, next)
            .isFalse();
      }
    }
  }
}
