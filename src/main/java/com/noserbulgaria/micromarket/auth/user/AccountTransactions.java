package com.noserbulgaria.micromarket.auth.user;

import com.noserbulgaria.micromarket.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.CustomerRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** DB-only side of {@link AccountService}. Split out so the orchestrator can call Stripe between transactions. */
@Service
@RequiredArgsConstructor
public class AccountTransactions {

  private final ProfileRepository profileRepository;
  private final CustomerRepository customerRepository;
  private final AccountMapper accountMapper;
  private final RefreshTokenService refreshTokenService;

  /** Email-update side effect that needs to be propagated to Stripe after the DB tx commits. */
  public record StripeEmailUpdate(UUID customerId, String stripeCustomerId, String newEmail) {}

  public record PatchResult(AccountResponse response, @Nullable StripeEmailUpdate stripeEmailUpdate) {}

  @Transactional(readOnly = true)
  public AccountResponse getByIdOrThrow(UUID userId) {
    return accountMapper.toDto(profileByUserIdOrThrow(userId));
  }

  @Transactional
  public PatchResult patchAccountOrThrow(UUID userId, AccountPatchRequest dto) {
    Profile profile = profileByUserIdOrThrow(userId);
    Customer customer = profile.getCustomer();
    User user = profile.getUser();

    applyEmailIfChanged(dto.email(), customer);
    applyRoleIfChanged(dto.role(), user);
    applyStatusIfChanged(dto.status(), user);

    Profile saved;
    try {
      saved = profileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException _) {
      throw new ConflictApiException(
          "Account with email '%s' already exists".formatted(dto.email()));
    }
    return new PatchResult(accountMapper.toDto(saved), pendingStripeSync(saved.getCustomer()));
  }

  /** Compare-and-clear: only flips the dirty flag off when the DB email still matches what we just pushed to Stripe. */
  @Transactional
  public void clearStripeEmailDirtyIfMatches(UUID customerId, String syncedEmail) {
    customerRepository.findById(customerId).ifPresent(c -> {
      if (syncedEmail.equals(c.getEmail())) {
        c.setStripeEmailDirty(false);
      }
    });
  }

  private void applyEmailIfChanged(@Nullable String email, Customer customer) {
    if (email == null) {
      return;
    }
    String normalized = email.toLowerCase(Locale.ROOT);
    if (normalized.equals(customer.getEmail())) {
      return;
    }
    if (customerRepository.findByEmail(normalized).isPresent()) {
      throw new ConflictApiException("Account with email '%s' already exists".formatted(email));
    }
    customer.setEmail(normalized);
  }

  private void applyRoleIfChanged(@Nullable Role role, User user) {
    if (role == null || user.getRole().equals(role)) {
      return;
    }
    refreshTokenService.revokeAllForUser(user.getId());
    user.setRole(role);
  }

  private void applyStatusIfChanged(@Nullable AccountStatus status, User user) {
    if (status == null) {
      return;
    }
    if (status == AccountStatus.INACTIVE && user.getStatus() != AccountStatus.INACTIVE) {
      refreshTokenService.revokeAllForUser(user.getId());
    }
    user.setStatus(status);
  }

  private @Nullable StripeEmailUpdate pendingStripeSync(Customer customer) {
    if (!customer.isStripeEmailDirty()) {
      return null;
    }
    String stripeCustomerId = customer.getStripeCustomerId();
    if (stripeCustomerId == null) {
      return null;
    }
    return new StripeEmailUpdate(customer.getId(), stripeCustomerId, customer.getEmail());
  }

  private Profile profileByUserIdOrThrow(UUID userId) {
    return profileRepository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundApiException("Account with id '%s' not found".formatted(userId)));
  }
}
