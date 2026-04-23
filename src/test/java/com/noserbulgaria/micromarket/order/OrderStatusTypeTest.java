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
    assertThat(OrderStatusType.PENDING_PAYMENT.canTransitionTo(OrderStatusType.REFUNDED)).isFalse();
  }

  @Test
  void paid_canOnlyTransitionTo_refunded() {
    assertThat(OrderStatusType.PAID.canTransitionTo(OrderStatusType.REFUNDED)).isTrue();
    assertThat(OrderStatusType.PAID.canTransitionTo(OrderStatusType.PAID)).isFalse();
    assertThat(OrderStatusType.PAID.canTransitionTo(OrderStatusType.CANCELLED)).isFalse();
    assertThat(OrderStatusType.PAID.canTransitionTo(OrderStatusType.PAYMENT_FAILED)).isFalse();
    assertThat(OrderStatusType.PAID.canTransitionTo(OrderStatusType.PENDING_PAYMENT)).isFalse();
  }

  @Test
  void refunded_canOnlyTransitionBackTo_paid() {
    assertThat(OrderStatusType.REFUNDED.canTransitionTo(OrderStatusType.PAID)).isTrue();
    assertThat(OrderStatusType.REFUNDED.canTransitionTo(OrderStatusType.REFUNDED)).isFalse();
    assertThat(OrderStatusType.REFUNDED.canTransitionTo(OrderStatusType.CANCELLED)).isFalse();
    assertThat(OrderStatusType.REFUNDED.canTransitionTo(OrderStatusType.PAYMENT_FAILED)).isFalse();
    assertThat(OrderStatusType.REFUNDED.canTransitionTo(OrderStatusType.PENDING_PAYMENT)).isFalse();
  }

  @Test
  void terminalStates_cannotTransition() {
    for (OrderStatusType terminal : new OrderStatusType[]{
        OrderStatusType.PAYMENT_FAILED, OrderStatusType.CANCELLED
    }) {
      for (OrderStatusType next : OrderStatusType.values()) {
        assertThat(terminal.canTransitionTo(next))
            .as("%s -> %s must not be allowed", terminal, next)
            .isFalse();
      }
    }
  }
}
