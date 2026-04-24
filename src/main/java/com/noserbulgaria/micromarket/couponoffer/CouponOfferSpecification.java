package com.noserbulgaria.micromarket.couponoffer;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.equalTo;

@UtilityClass
public class CouponOfferSpecification {

  public static Specification<CouponOffer> withFilter(CouponOfferFilter filter) {
    return Specification.allOf(equalTo(CouponOffer_.active, filter.active()));
  }

  public static Specification<CouponOffer> availableAt(Instant now) {
    return Specification.allOf(
        equalTo(CouponOffer_.active, true),
        startsBefore(now),
        expiresAfter(now),
        hasPurchaseCapacity()
    );
  }

  private static Specification<CouponOffer> startsBefore(Instant now) {
    return (root, _, cb) -> cb.or(
        cb.isNull(root.get(CouponOffer_.startDate)),
        cb.lessThanOrEqualTo(root.get(CouponOffer_.startDate), now)
    );
  }

  private static Specification<CouponOffer> expiresAfter(Instant now) {
    return (root, _, cb) -> cb.or(
        cb.isNull(root.get(CouponOffer_.expiryDate)),
        cb.greaterThan(root.get(CouponOffer_.expiryDate), now)
    );
  }

  private static Specification<CouponOffer> hasPurchaseCapacity() {
    return (root, _, cb) -> cb.or(
        cb.isNull(root.get(CouponOffer_.maxPurchases)),
        cb.lessThan(root.get(CouponOffer_.purchaseCount), root.get(CouponOffer_.maxPurchases))
    );
  }
}
