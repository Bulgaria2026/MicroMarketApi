package com.noserbulgaria.micromarket.security.auth.refresh;

import com.noserbulgaria.micromarket.domain.profile.Profile;
import com.noserbulgaria.micromarket.domain.profile.ProfileRepository;
import com.noserbulgaria.micromarket.security.user.AccountStatus;
import com.noserbulgaria.micromarket.security.user.Role;
import com.noserbulgaria.micromarket.security.user.User;
import com.noserbulgaria.micromarket.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
  private ProfileRepository profileRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private UUID userId;

  @BeforeEach
  void setUp() {
    cleanDatabase();

    User user = new User();
    user.setEmail("cleanup@micromarket.dev");
    user.setPassword(Objects.requireNonNull(passwordEncoder.encode("user123")));
    user.setRole(Role.USER);
    user.setStatus(AccountStatus.ACTIVE);
    user = userRepository.save(user);

    Profile profile = new Profile();
    profile.setUser(user);
    profile.setPoints(0);
    profileRepository.save(profile);

    userId = user.getId();
  }

  private void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM refresh_tokens");
    jdbcTemplate.update("DELETE FROM order_item");
    jdbcTemplate.update("DELETE FROM orders");
    jdbcTemplate.update("DELETE FROM profile");
    jdbcTemplate.update("DELETE FROM guest");
    jdbcTemplate.update("DELETE FROM users");
    jdbcTemplate.update("DELETE FROM customer");
    jdbcTemplate.update("DELETE FROM product");
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

    assertEquals(
        Set.of(fresh, recentlyRevoked, expiredWithinGrace), remaining,
        "Cleanup must keep fresh, recently-revoked, and still-in-grace rows; delete others"
    );
    assertEquals(
        List.of(), refreshTokenRepository.findAll().stream()
            .map(RefreshToken::getJti)
            .filter(jti -> jti.equals(longExpired) || jti.equals(longRevoked))
            .toList()
    );
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

    void revokedAt(Instant value) {
      this.revokedAt = value;
    }
  }
}
