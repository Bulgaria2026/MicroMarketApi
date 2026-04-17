package com.noserbulgaria.micromarket.security.auth.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenCleanupProperties properties;

  /**
   * Deletes refresh tokens that have been expired or revoked for longer than the configured
   * grace window. The grace keeps recently revoked rows around long enough that a replayed
   * token still triggers reuse detection instead of silently 401'ing as "unknown".
   */
  @Scheduled(cron = "${auth.refresh-token.cleanup-cron}")
  @Transactional
  public void purgeStale() {
    Instant cutoff = Instant.now().minus(properties.getCleanupGrace());
    int deleted = refreshTokenRepository.deleteStaleTokens(cutoff);
    if (deleted > 0) {
      log.info("Purged {} stale refresh token(s) older than {}", deleted, cutoff);
    }
  }
}
