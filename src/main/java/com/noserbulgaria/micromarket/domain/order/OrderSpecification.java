package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.generic.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.equalTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.greaterThanOrEqualTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.lessThanOrEqualTo;

@UtilityClass
public class OrderSpecification {

  public static Specification<Order> withFilter(OrderFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.fromDate()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.toDate()),
        filter.customerId() == null ? Specification.unrestricted()
            : (root, _, cb) -> cb.equal(root.join(Order_.customer).get("id"), filter.customerId()),
        equalTo(Order_.status, filter.status())
    );
  }
}
