package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.exception.BadRequestApiException;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerResolver {

  private final CustomerRepository customerRepository;

  /** Returns the Customer for the checkout: the caller's own if authenticated, otherwise a Guest by email. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Customer resolveForCheckout(@Nullable CustomUserDetails userDetails, @Nullable String email) {
    if (userDetails != null) {
      return customerRepository.findByEmail(userDetails.getEmail())
          .orElseThrow(() -> new IllegalStateException(
              "Authenticated user '%s' has no Customer row".formatted(userDetails.getEmail())));
    }
    if (email == null || email.isBlank()) {
      throw new BadRequestApiException("Email is required for guest checkout");
    }
    return resolveGuestByEmail(email.trim().toLowerCase(Locale.ROOT));
  }

  private Customer resolveGuestByEmail(String email) {
    return customerRepository.findByEmail(email)
        .map(this::rejectIfRegistered)
        .orElseGet(() -> createGuestOrReload(email));
  }

  private Customer createGuestOrReload(String email) {
    Customer guest = new Customer();
    guest.setEmail(email);
    try {
      return customerRepository.saveAndFlush(guest);
    } catch (DataIntegrityViolationException ex) {
      return customerRepository.findByEmail(email).map(this::rejectIfRegistered).orElseThrow(() -> ex);
    }
  }

  private Customer rejectIfRegistered(Customer customer) {
    if (customer.isRegistered()) {
      throw new ConflictApiException(
          "Email '%s' is registered, please log in to place an order".formatted(customer.getEmail()));
    }
    return customer;
  }
}
