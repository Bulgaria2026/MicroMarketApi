package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.generic.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.*;

@UtilityClass
public class OrderSpecification {

  public static Specification<Order> withFilter(OrderFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.fromDate()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.toDate()),
        equalTo(Order_.customerId, filter.customerId()),
        equalTo(Order_.status, filter.status())
    );
  }
}
