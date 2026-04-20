package com.noserbulgaria.micromarket.security.user;

import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.containsIgnoreCase;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.equalTo;

public final class UserSpecification {

  private UserSpecification() {
  }

  public static Specification<User> withFilter(UserFilter filter) {
    return Specification.allOf(
        containsIgnoreCase(User_.email, filter.email()),
        equalTo(User_.role, filter.role()),
        equalTo(User_.status, filter.status())
    );
  }
}
