package com.noserbulgaria.micromarket.couponoffer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CouponOfferRepository extends JpaRepository<CouponOffer, UUID>, JpaSpecificationExecutor<CouponOffer> {
}
