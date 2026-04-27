package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User_;
import com.noserbulgaria.micromarket.common.ExtendedEntity_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.greaterThanOrEqualTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.lessThanOrEqualTo;

@UtilityClass
public class CustomerSpecification {

  public static Specification<Customer> withFilter(CustomerFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(ExtendedEntity_.createdAt, filter.createdFrom()),
        lessThanOrEqualTo(ExtendedEntity_.createdAt, filter.createdTo()),
        emailContains(filter.email()),
        typeEquals(filter.type()),
        roleEquals(filter.role()),
        statusEquals(filter.status()),
        pointsBetween(filter.minPoints(), filter.maxPoints())
    );
  }

  private static Specification<Customer> typeEquals(@Nullable CustomerType type) {
    if (type == null) return Specification.unrestricted();
    return (root, query, cb) -> {
      var registered = query.subquery(java.util.UUID.class);
      var profileRoot = registered.from(Profile.class);
      registered.select(profileRoot.get(Profile_.customer).get(ExtendedEntity_.id));
      var inRegistered = root.get(ExtendedEntity_.id).in(registered);
      return type == CustomerType.PROFILE ? inRegistered : cb.not(inRegistered);
    };
  }

  private static Specification<Customer> emailContains(@Nullable String email) {
    if (email == null) return Specification.unrestricted();
    String escaped = email.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    String pattern = "%" + escaped.toLowerCase() + "%";
    return (root, _, cb) -> cb.like(cb.lower(root.get(Customer_.email)), pattern, '\\');
  }

  private static Specification<Customer> roleEquals(@Nullable Role role) {
    if (role == null) return Specification.unrestricted();
    return (root, _, cb) -> cb.equal(
        root.get(Customer_.profile).get(Profile_.user).get(User_.role), role);
  }

  private static Specification<Customer> statusEquals(@Nullable AccountStatus status) {
    if (status == null) return Specification.unrestricted();
    return (root, _, cb) -> cb.equal(
        root.get(Customer_.profile).get(Profile_.user).get(User_.status), status);
  }

  private static Specification<Customer> pointsBetween(@Nullable Long min, @Nullable Long max) {
    if (min == null && max == null) return Specification.unrestricted();
    if (min != null && max != null) {
      return (root, _, cb) -> cb.between(root.get(Customer_.profile).get(Profile_.points), min, max);
    }
    if (min != null) {
      return (root, _, cb) -> cb.greaterThanOrEqualTo(root.get(Customer_.profile).get(Profile_.points), min);
    }
    return (root, _, cb) -> cb.lessThanOrEqualTo(root.get(Customer_.profile).get(Profile_.points), max);
  }
}
