package com.noserbulgaria.micromarket.coupon;

import com.noserbulgaria.micromarket.common.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@UtilityClass
public class CouponSpecification {

  public static Specification<Coupon> withFilter(CouponFilter filter) {
    return Specification.allOf(userIdEquals(filter.userId()));
  }

  private static Specification<Coupon> userIdEquals(@Nullable UUID userId) {
    if (userId == null) {
      return Specification.unrestricted();
    }
    return (root, _, cb) -> cb.equal(root.get(Coupon_.user).get(ExtendedEntity_.id), userId);
  }
}
