package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeCustomerTransactions {

  private final CustomerRepository customerRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<String> findStripeCustomerId(UUID customerId) {
    return customerRepository.findById(customerId)
        .map(Customer::getStripeCustomerId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String storeStripeCustomerId(UUID customerId, String stripeCustomerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new NotFoundApiException("Customer with id '%s' not found".formatted(customerId)));
    String existing = customer.getStripeCustomerId();
    if (existing != null) {
      return existing;
    }
    customer.setStripeCustomerId(stripeCustomerId);
    customerRepository.saveAndFlush(customer);
    return stripeCustomerId;
  }
}
