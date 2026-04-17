package com.noserbulgaria.micromarket.security.auth.refresh;

import com.noserbulgaria.micromarket.generic.ExtendedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted refresh-token record keyed by the JWT {@code jti}. All tokens issued in the same
 * login session share a {@code familyId}, which lets the rotation flow revoke an entire family
 * when token reuse is detected.
 */
@Data
@Entity
@Table(name = "refresh_tokens")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("NullAway.Init")
public class RefreshToken extends ExtendedEntity {

  @Column(nullable = false, unique = true)
  private UUID jti;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private UUID familyId;

  @Column(nullable = false)
  private Instant expiresAt;

  private @Nullable Instant revokedAt;
}
