package com.noserbulgaria.micromarket.security.auth.refresh;

import com.noserbulgaria.micromarket.domain.customer.Customer;
import com.noserbulgaria.micromarket.domain.order.OrderRepository;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenCleanupJobTest {

  @Autowired
  private RefreshTokenCleanupJob cleanupJob;

  @Autowired
  private RefreshTokenCleanupProperties properties;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private UUID userId;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    orderRepository.deleteAll();
    profileRepository.deleteAll();
    userRepository.deleteAll();

    User user = new User();
    user.setCustomer(new Customer());
    user.setEmail("cleanup@micromarket.dev");
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode("user123")));
    user.setRole(Role.USER);
    userId = userRepository.save(user).getId();
  }

  @Test
  void purgeStale_keepsFreshAndRecentlyRevoked_deletesStaleAndLongExpired() {
    Duration grace = properties.getCleanupGrace();
    Instant now = Instant.now();
    Duration beyondGrace = grace.plusDays(1);
    Duration withinGrace = grace.minusDays(1);

    UUID fresh = insert(b -> b
        .expiresAt(now.plus(Duration.ofDays(1)))
        .revokedAt(null));

    UUID recentlyRevoked = insert(b -> b
        .expiresAt(now.plus(Duration.ofDays(1)))
        .revokedAt(now.minus(withinGrace)));

    UUID expiredWithinGrace = insert(b -> b
        .expiresAt(now.minus(withinGrace))
        .revokedAt(null));

    UUID longExpired = insert(b -> b
        .expiresAt(now.minus(beyondGrace))
        .revokedAt(null));

    UUID longRevoked = insert(b -> b
        .expiresAt(now.plus(Duration.ofDays(1)))
        .revokedAt(now.minus(beyondGrace)));

    cleanupJob.purgeStale();

    Set<UUID> remaining = refreshTokenRepository.findAll().stream()
        .map(RefreshToken::getJti)
        .collect(Collectors.toUnmodifiableSet());

    assertEquals(Set.of(fresh, recentlyRevoked, expiredWithinGrace), remaining,
        "Cleanup must keep fresh, recently-revoked, and still-in-grace rows; delete others");
    assertEquals(List.of(), refreshTokenRepository.findAll().stream()
        .map(RefreshToken::getJti)
        .filter(jti -> jti.equals(longExpired) || jti.equals(longRevoked))
        .toList());
  }

  private UUID insert(java.util.function.Consumer<TokenAttrs> customizer) {
    TokenAttrs attrs = new TokenAttrs();
    customizer.accept(attrs);

    RefreshToken token = new RefreshToken();
    token.setJti(UUID.randomUUID());
    token.setUserId(userId);
    token.setFamilyId(UUID.randomUUID());
    token.setExpiresAt(attrs.expiresAt);
    token.setRevokedAt(attrs.revokedAt);
    refreshTokenRepository.save(token);
    return token.getJti();
  }

  private static class TokenAttrs {
    Instant expiresAt = Instant.now();
    Instant revokedAt;

    TokenAttrs expiresAt(Instant value) {
      this.expiresAt = value;
      return this;
    }

    TokenAttrs revokedAt(Instant value) {
      this.revokedAt = value;
      return this;
    }
  }
}
