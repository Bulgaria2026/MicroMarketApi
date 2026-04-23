package com.noserbulgaria.micromarket.product;

import com.noserbulgaria.micromarket.auth.user.CustomUserDetails;
import com.noserbulgaria.micromarket.auth.user.Role;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.*;

@UtilityClass
public class ProductSpecification {

  public static Specification<Product> forRole(ProductFilter filter, @Nullable CustomUserDetails userDetails) {
    boolean isAdmin = userDetails != null && userDetails.getRole() == Role.ADMINISTRATOR;
    return withFilter(filter)
        .and(equalTo(Product_.enabled, isAdmin ? filter.enabled() : Boolean.TRUE));
  }

  private static Specification<Product> withFilter(ProductFilter filter) {
    return Specification.allOf(
        containsIgnoreCase(Product_.name, filter.name()),
        greaterThanOrEqualTo(Product_.price, filter.minPrice()),
        lessThanOrEqualTo(Product_.price, filter.maxPrice())
    );
  }
}
