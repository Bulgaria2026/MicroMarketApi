package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeCustomerServiceTest {

  @Mock
  private StripeCustomerTransactions transactions;
  @Mock
  private StripePaymentProvider stripePaymentProvider;

  @Test
  void ensureStripeCustomer_existingCustomerIdDoesNotCallStripe() {
    UUID customerId = UUID.randomUUID();
    StripeCustomerService service = new StripeCustomerService(transactions, stripePaymentProvider);
    when(transactions.findStripeCustomerId(customerId)).thenReturn(Optional.of("cus_existing"));

    String result = service.ensureStripeCustomer(customerId, "customer@example.com");

    assertThat(result).isEqualTo("cus_existing");
    verify(stripePaymentProvider, never()).createCustomer("customer@example.com", customerId.toString());
  }

  @Test
  void ensureStripeCustomer_optimisticLoserReloadsWinningCustomerId() {
    UUID customerId = UUID.randomUUID();
    StripeCustomerService service = new StripeCustomerService(transactions, stripePaymentProvider);
    when(transactions.findStripeCustomerId(customerId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of("cus_winner"));
    when(stripePaymentProvider.createCustomer("customer@example.com", customerId.toString()))
        .thenReturn("cus_winner");
    when(transactions.storeStripeCustomerId(customerId, "cus_winner"))
        .thenThrow(new OptimisticLockingFailureException("lost race"));

    String result = service.ensureStripeCustomer(customerId, "customer@example.com");

    assertThat(result).isEqualTo("cus_winner");
    verify(stripePaymentProvider).createCustomer("customer@example.com", customerId.toString());
  }
}
