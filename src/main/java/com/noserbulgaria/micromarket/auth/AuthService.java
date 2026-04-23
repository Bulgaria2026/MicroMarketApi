package com.noserbulgaria.micromarket.auth;

import com.noserbulgaria.micromarket.auth.refresh.RefreshTokenService;
import com.noserbulgaria.micromarket.auth.refresh.RotationResult;
import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.customer.Customer;
import com.noserbulgaria.micromarket.customer.Guest;
import com.noserbulgaria.micromarket.customer.GuestRepository;
import com.noserbulgaria.micromarket.customer.Profile;
import com.noserbulgaria.micromarket.customer.ProfileRepository;
import com.noserbulgaria.micromarket.exception.ConflictApiException;
import com.noserbulgaria.micromarket.exception.NotFoundApiException;
import com.noserbulgaria.micromarket.order.Order;
import com.noserbulgaria.micromarket.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;
  private final GuestRepository guestRepository;
  private final OrderRepository orderRepository;
  private final ProfileRepository profileRepository;

  @Transactional
  public AuthTokens register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictApiException("User with email '%s' already exists".formatted(request.email()));
    }

    User user = new User();
    user.setEmail(request.email());
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode(request.password())));
    user.setRole(Role.USER);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.save(user);

    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(0);

    Customer registeredCustomer = profileRepository.save(profile);

    Optional<Guest> existingGuest = guestRepository.findByEmailIgnoreCase(request.email());
    if (existingGuest.isPresent()) {
      Guest guest = existingGuest.get();
      Set<Order> guestOrders = guest.getOrders();
      guestOrders.forEach(order -> order.setCustomer(registeredCustomer));
      orderRepository.saveAll(guestOrders);
      guestRepository.delete(guest);
    }

    return buildAuthTokens(user, refreshTokenService.issueForNewFamily(user));
  }

  @Transactional
  public AuthTokens login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new NotFoundApiException("User with email '%s' not found".formatted(request.email())));
    return buildAuthTokens(user, refreshTokenService.issueForNewFamily(user));
  }

  public AuthTokens refresh(String rawRefreshToken) {
    RotationResult result = refreshTokenService.rotate(rawRefreshToken);
    return buildAuthTokens(result.user(), result.newRawToken());
  }

  public void revokeFamilyFromToken(@Nullable String rawRefreshToken) {
    refreshTokenService.revokeFamilyOfToken(rawRefreshToken);
  }

  private AuthTokens buildAuthTokens(User user, String refreshToken) {
    String accessToken = tokenService.generateAccessToken(user);
    long expiresIn = tokenService.getAccessTokenExpirationSeconds();
    return new AuthTokens(accessToken, refreshToken, expiresIn);
  }
}
