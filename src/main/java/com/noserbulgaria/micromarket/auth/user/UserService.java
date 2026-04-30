package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Non-transactional orchestrator — Stripe email propagation runs after the DB tx commits so it holds no DB locks. */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserTransactions transactions;
  private final StripePaymentProvider stripePaymentProvider;

  public UserResponse getByIdOrThrow(UUID userId) {
    return transactions.getByIdOrThrow(userId);
  }

  public UserResponse patchUserOrThrow(UUID userId, UserPatchRequest dto) {
    UserTransactions.PatchResult result = transactions.patchUserOrThrow(userId, dto);
    UserTransactions.StripeEmailUpdate stripeEmailUpdate = result.stripeEmailUpdate();
    if (stripeEmailUpdate != null) {
      stripePaymentProvider.updateCustomerEmail(stripeEmailUpdate.stripeCustomerId(), stripeEmailUpdate.newEmail());
    }
    return result.response();
  }
}
