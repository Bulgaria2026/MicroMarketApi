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

/** DB-only side of {@link UserService}. Split out so the orchestrator can call Stripe between transactions. */
@Service
@RequiredArgsConstructor
public class UserTransactions {

  private final ProfileRepository profileRepository;
  private final CustomerRepository customerRepository;
  private final UserMapper userMapper;
  private final RefreshTokenService refreshTokenService;

  /** Email-update side effect that needs to be propagated to Stripe after the DB tx commits. */
  public record StripeEmailUpdate(String stripeCustomerId, String newEmail) {}

  public record PatchResult(UserResponse response, @Nullable StripeEmailUpdate stripeEmailUpdate) {}

  @Transactional(readOnly = true)
  public UserResponse getByIdOrThrow(UUID userId) {
    return userMapper.toDto(profileByUserIdOrThrow(userId));
  }

  @Transactional
  public PatchResult patchUserOrThrow(UUID userId, UserPatchRequest dto) {
    Profile profile = profileByUserIdOrThrow(userId);
    Customer customer = profile.getCustomer();
    User user = profile.getUser();

    StripeEmailUpdate stripeEmailUpdate = applyEmailIfChanged(dto.email(), customer);
    applyRoleIfChanged(dto.role(), user);
    applyStatusIfChanged(dto.status(), user);

    Profile saved;
    try {
      saved = profileRepository.saveAndFlush(profile);
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException _) {
      throw new ConflictApiException(
          "User with email '%s' already exists".formatted(dto.email()));
    }
    return new PatchResult(userMapper.toDto(saved), stripeEmailUpdate);
  }

  private @Nullable StripeEmailUpdate applyEmailIfChanged(@Nullable String email, Customer customer) {
    if (email == null) {
      return null;
    }
    String normalized = email.toLowerCase(Locale.ROOT);
    if (normalized.equals(customer.getEmail())) {
      return null;
    }
    if (customerRepository.findByEmail(normalized).isPresent()) {
      throw new ConflictApiException("User with email '%s' already exists".formatted(email));
    }
    customer.setEmail(normalized);
    String stripeCustomerId = customer.getStripeCustomerId();
    return stripeCustomerId == null ? null : new StripeEmailUpdate(stripeCustomerId, normalized);
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

  private Profile profileByUserIdOrThrow(UUID userId) {
    return profileRepository.findByUserId(userId)
        .orElseThrow(() -> new NotFoundApiException("User with id '%s' not found".formatted(userId)));
  }
}
