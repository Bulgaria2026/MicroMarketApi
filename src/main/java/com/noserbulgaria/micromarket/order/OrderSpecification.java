package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.common.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.containsIgnoreCase;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.equalTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.greaterThanOrEqualTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.lessThanOrEqualTo;

@UtilityClass
public class OrderSpecification {

  public static Specification<Order> withFilter(OrderFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.fromDate()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.toDate()),
        customerIdEquals(filter.customerId()),
        containsIgnoreCase(Order_.orderNumber, filter.orderNumber()),
        containsIgnoreCase(Order_.email, filter.email()),
        equalTo(Order_.status, filter.status())
    );
  }

  public static Specification<Order> forCustomer(UUID customerId, OwnOrderFilter filter) {
    return Specification.allOf(
        customerIdEquals(customerId),
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.fromDate()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.toDate())
    );
  }

  private static Specification<Order> customerIdEquals(@Nullable UUID customerId) {
    if (customerId == null) {
      return Specification.unrestricted();
    }
    return (root, _, cb) -> cb.equal(root.get(Order_.customer).get(ExtendedEntity_.id), customerId);
  }
}
