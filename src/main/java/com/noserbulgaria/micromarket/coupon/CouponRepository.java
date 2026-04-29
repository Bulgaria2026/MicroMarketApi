package com.noserbulgaria.micromarket.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID>, JpaSpecificationExecutor<Coupon> {

  boolean existsByCode(String code);

  Optional<Coupon> findByStripePromotionCodeId(String stripePromotionCodeId);
}
