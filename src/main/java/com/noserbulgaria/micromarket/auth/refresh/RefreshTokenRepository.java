package com.noserbulgaria.micromarket.auth.refresh;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshToken> findByJti(UUID jti);

  @Modifying
  @Query("""
      update RefreshToken t
      set t.revokedAt = :revokedAt
      where t.familyId = :familyId and t.revokedAt is null
      """)
  void revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

  @Modifying
  @Query("""
      delete from RefreshToken t
      where t.expiresAt < :cutoff or t.revokedAt < :cutoff
      """)
  int deleteStaleTokens(@Param("cutoff") Instant cutoff);
}
