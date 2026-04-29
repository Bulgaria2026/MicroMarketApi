package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeCustomerService {

  private final StripeCustomerTransactions transactions;
  private final StripePaymentProvider stripePaymentProvider;

  public String ensureStripeCustomer(UUID customerId, String email) {
    return transactions.findStripeCustomerId(customerId)
        .orElseGet(() -> createAndStoreStripeCustomer(customerId, email));
  }

  private String createAndStoreStripeCustomer(UUID customerId, String email) {
    String stripeCustomerId = stripePaymentProvider.createCustomer(email, customerId.toString());
    try {
      return transactions.storeStripeCustomerId(customerId, stripeCustomerId);
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException ex) {
      return transactions.findStripeCustomerId(customerId)
          .orElseThrow(() -> ex);
    }
  }
}
