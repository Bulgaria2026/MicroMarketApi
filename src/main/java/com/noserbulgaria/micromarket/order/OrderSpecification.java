package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.common.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.*;

@UtilityClass
public class OrderSpecification {

  public static Specification<Order> withFilter(OrderFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.fromDate()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.toDate()),
        customerIdEquals(filter.customerId()),
        equalTo(Order_.status, filter.status())
    );
  }

  private static Specification<Order> customerIdEquals(@Nullable UUID customerId) {
    if (customerId == null) {
      return Specification.unrestricted();
    }
    return (root, _, cb) -> cb.equal(root.get(Order_.customer).get(ExtendedEntity_.id), customerId);
  }
}
