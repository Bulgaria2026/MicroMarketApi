package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.payment.stripe.StripePaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Non-transactional orchestrator — Stripe email propagation runs after the DB tx commits so it holds no DB locks. */
@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountTransactions transactions;
  private final StripePaymentProvider stripePaymentProvider;

  public AccountResponse getByIdOrThrow(UUID userId) {
    return transactions.getByIdOrThrow(userId);
  }

  public AccountResponse patchAccountOrThrow(UUID userId, AccountPatchRequest dto) {
    AccountTransactions.PatchResult result = transactions.patchAccountOrThrow(userId, dto);
    AccountTransactions.StripeEmailUpdate pending = result.stripeEmailUpdate();
    if (pending != null) {
      stripePaymentProvider.updateCustomerEmail(pending.stripeCustomerId(), pending.newEmail());
      transactions.clearStripeEmailDirtyIfMatches(pending.customerId(), pending.newEmail());
    }
    return result.response();
  }
}
