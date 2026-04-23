package com.noserbulgaria.micromarket.auth.refresh;

import com.noserbulgaria.micromarket.auth.TokenService;
import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.User;
import com.noserbulgaria.micromarket.auth.user.UserRepository;
import com.noserbulgaria.micromarket.exception.UnauthorizedApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns the full refresh-token lifecycle: issuing new tokens, rotating on refresh, detecting
 * reuse (theft signal), and revoking families on logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

  private static final String INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";
  private static final String UNKNOWN = "unknown";

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final TokenService tokenService;

  /**
   * Issues the first refresh token of a new session (new family id).
   */
  public String issueForNewFamily(User user) {
    return issueInFamily(user, UUID.randomUUID());
  }

  /**
   * Rotates the presented refresh token and detects reuse.
   *
   * <p>On success the presented token is marked revoked, and a new token is issued in the same
   * family. If the presented token was already revoked, this is a theft signal: the entire
   * family is revoked and a 401 is thrown. The revoke must survive the thrown exception,
   * which is why this method disables rollback for {@link UnauthorizedApiException}.
   *
   * @throws UnauthorizedApiException if the token is invalid, expired, unknown, or reused
   */
  @Transactional(noRollbackFor = UnauthorizedApiException.class)
  public RotationResult rotate(String rawRefreshToken) {
    Jwt jwt = tokenService.decodeRefreshToken(rawRefreshToken);
    UUID jti = parseJti(jwt);

    RefreshToken presented = refreshTokenRepository.findByJti(jti)
        .orElseThrow(() -> new UnauthorizedApiException(INVALID_REFRESH_TOKEN));

    Instant now = Instant.now();

    if (presented.getRevokedAt() != null) {
      logReuseDetected(presented, now);
      refreshTokenRepository.revokeFamily(presented.getFamilyId(), now);
      throw new UnauthorizedApiException(INVALID_REFRESH_TOKEN);
    }

    if (!presented.getExpiresAt().isAfter(now)) {
      throw new UnauthorizedApiException(INVALID_REFRESH_TOKEN);
    }

    User user = userRepository.findById(presented.getUserId())
        .orElseThrow(() -> new UnauthorizedApiException(INVALID_REFRESH_TOKEN));

    if (user.getStatus() != AccountStatus.ACTIVE) {
      revokeAllForUser(user.getId());
      throw new UnauthorizedApiException(INVALID_REFRESH_TOKEN);
    }

    presented.setRevokedAt(now);
    String newRawToken = issueInFamily(user, presented.getFamilyId());
    return new RotationResult(user, newRawToken);
  }

  /**
   * Best-effort revoke of the family belonging to the given refresh token. Used by logout,
   * which must stay idempotent: a missing, blank, or already-invalid token is a no-op rather
   * than an error.
   */
  public void revokeFamilyOfToken(@Nullable String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    Jwt jwt;
    try {
      jwt = tokenService.decodeRefreshToken(rawRefreshToken);
    } catch (UnauthorizedApiException _) {
      return;
    }
    UUID jti = parseJti(jwt);
    refreshTokenRepository.findByJti(jti)
        .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
  }

  public void revokeAllForUser(UUID userId) {
    refreshTokenRepository.revokeAllForUser(userId, Instant.now());
  }

  private String issueInFamily(User user, UUID familyId) {
    UUID jti = UUID.randomUUID();
    String rawToken = tokenService.generateRefreshToken(user, jti);

    RefreshToken tokenRecord = new RefreshToken();
    tokenRecord.setJti(jti);
    tokenRecord.setUserId(user.getId());
    tokenRecord.setFamilyId(familyId);
    tokenRecord.setExpiresAt(Instant.now().plus(tokenService.getRefreshTokenExpiration()));
    refreshTokenRepository.save(tokenRecord);

    return rawToken;
  }

  private void logReuseDetected(RefreshToken presented, Instant now) {
    HttpServletRequest request = currentRequest();
    log.warn(
        "Refresh token reuse detected — revoking family. "
            + "userId={} familyId={} jti={} originalRevokedAt={} replayedAt={} "
            + "remoteAddr={} userAgent={} requestUri={}",
        presented.getUserId(),
        presented.getFamilyId(),
        presented.getJti(),
        presented.getRevokedAt(),
        now,
        request != null ? request.getRemoteAddr() : UNKNOWN,
        request != null ? request.getHeader("User-Agent") : UNKNOWN,
        request != null ? request.getRequestURI() : UNKNOWN);
  }

  private @Nullable HttpServletRequest currentRequest() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return attrs.getRequest();
    }
    return null;
  }

  private UUID parseJti(Jwt jwt) {
    String id = jwt.getId();
    if (id == null) {
      throw new UnauthorizedApiException(INVALID_REFRESH_TOKEN);
    }
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException _) {
      throw new UnauthorizedApiException(INVALID_REFRESH_TOKEN);
    }
  }
}
