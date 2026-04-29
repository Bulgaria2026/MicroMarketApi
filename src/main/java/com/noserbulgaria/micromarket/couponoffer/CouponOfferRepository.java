package com.noserbulgaria.micromarket.couponoffer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponOfferRepository extends JpaRepository<CouponOffer, UUID>, JpaSpecificationExecutor<CouponOffer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from CouponOffer c where c.id = :id")
  Optional<CouponOffer> findByIdForUpdate(@Param("id") UUID id);
}
