package com.noserbulgaria.micromarket.payment.stripe.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface StripeEventRepository extends JpaRepository<StripeEvent, String> {

  @Modifying
  @Query("delete from StripeEvent e where e.receivedAt < :cutoff")
  int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
