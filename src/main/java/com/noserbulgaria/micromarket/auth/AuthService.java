package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.auth.refresh.RotationResult;
import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;
  private final CustomerRepository customerRepository;
  private final ProfileRepository profileRepository;

  @Transactional
  public AuthTokens register(RegisterRequest request) {
    String email = request.email().toLowerCase(Locale.ROOT);

    Customer customer = customerRepository.findByEmail(email).orElseGet(() -> {
      Customer fresh = new Customer();
      fresh.setEmail(email);
      return fresh;
    });
    if (customer.isRegistered()) {
      throw new ConflictApiException("User with email '%s' already exists".formatted(request.email()));
    }

    User user = new User();
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
    user.setRole(Role.USER);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.save(user);

    Profile profile = new Profile();
    profile.setCustomer(customer);
    profile.setUser(user);
    profile.setPoints(0);
    customer.setProfile(profile);
    try {
      customerRepository.saveAndFlush(customer);
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException _) {
      throw new ConflictApiException(
          "User with email '%s' already exists".formatted(request.email()));
    }

    return buildAuthTokens(user, customer.getEmail(), refreshTokenService.issueForNewFamily(user));
  }

  @Transactional
  public AuthTokens login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    Profile profile = profileRepository.findByCustomer_Email(request.email().toLowerCase(Locale.ROOT))
        .orElseThrow(() -> new NotFoundApiException(
            "User with email '%s' not found".formatted(request.email())));
    User user = profile.getUser();
    return buildAuthTokens(user, profile.getCustomer().getEmail(),
        refreshTokenService.issueForNewFamily(user));
  }

  public AuthTokens refresh(String rawRefreshToken) {
    RotationResult result = refreshTokenService.rotate(rawRefreshToken);
    Profile profile = profileRepository.findByUserId(result.user().getId())
        .orElseThrow(() -> new NotFoundApiException(
            "Profile for user '%s' not found".formatted(result.user().getId())));
    return buildAuthTokens(result.user(), profile.getCustomer().getEmail(), result.newRawToken());
  }

  public void revokeFamilyFromToken(@Nullable String rawRefreshToken) {
    refreshTokenService.revokeFamilyOfToken(rawRefreshToken);
  }

  private AuthTokens buildAuthTokens(User user, String email, String refreshToken) {
    String accessToken = tokenService.generateAccessToken(user, email);
    return new AuthTokens(accessToken, refreshToken, tokenService.getAccessTokenExpirationSeconds());
  }
}
