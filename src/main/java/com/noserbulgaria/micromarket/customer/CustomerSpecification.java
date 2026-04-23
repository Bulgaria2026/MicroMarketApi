package com.noserbulgaria.micromarket.customer;

import com.noserbulgaria.micromarket.auth.user.AccountStatus;
import com.noserbulgaria.micromarket.auth.user.Role;
import com.noserbulgaria.micromarket.auth.user.User_;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import static com.noserbulgaria.micromarket.common.SpecificationBuilder.greaterThanOrEqualTo;
import static com.noserbulgaria.micromarket.common.SpecificationBuilder.lessThanOrEqualTo;

@UtilityClass
public class CustomerSpecification {

  public static Specification<Customer> withFilter(CustomerFilter filter) {
    return Specification.allOf(
        greaterThanOrEqualTo(Customer_.createdAt, filter.createdFrom()),
        lessThanOrEqualTo(Customer_.createdAt, filter.createdTo()),
        emailContains(filter.email()),
        typeEquals(filter.type()),
        profileRoleEquals(filter.role()),
        profileStatusEquals(filter.status()),
        profilePointsBetween(filter.minPoints(), filter.maxPoints())
    );
  }

  private static Specification<Customer> typeEquals(@Nullable CustomerType type) {
    if (type == null) {
      return Specification.unrestricted();
    }

    return switch (type) {
      case GUEST -> (root, _, cb) -> cb.equal(root.type(), Guest.class);
      case PROFILE -> (root, _, cb) -> cb.equal(root.type(), Profile.class);
    };
  }

  private static Specification<Customer> emailContains(@Nullable String email) {
    if (email == null) {
      return Specification.unrestricted();
    }

    String escaped = email.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    String pattern = "%" + escaped.toLowerCase() + "%";

    return (root, query, cb) -> {
      var guestMatches = query.subquery(java.util.UUID.class);
      var guestRoot = guestMatches.from(Guest.class);
      guestMatches.select(guestRoot.get(Guest_.id))
          .where(cb.like(cb.lower(guestRoot.get(Guest_.email)), pattern, '\\'));

      var profileMatches = query.subquery(java.util.UUID.class);
      var profileRoot = profileMatches.from(Profile.class);
      profileMatches.select(profileRoot.get(Profile_.id))
          .where(cb.like(cb.lower(profileRoot.get(Profile_.user).get(User_.email)), pattern, '\\'));

      return cb.or(
          root.get(Customer_.id).in(guestMatches),
          root.get(Customer_.id).in(profileMatches)
      );
    };
  }

  private static Specification<Customer> profileRoleEquals(@Nullable Role role) {
    if (role == null) {
      return Specification.unrestricted();
    }

    return (root, query, cb) -> {
      var profileMatches = query.subquery(java.util.UUID.class);
      var profileRoot = profileMatches.from(Profile.class);
      profileMatches.select(profileRoot.get(Profile_.id))
          .where(cb.equal(profileRoot.get(Profile_.user).get(User_.role), role));
      return root.get(Customer_.id).in(profileMatches);
    };
  }

  private static Specification<Customer> profileStatusEquals(@Nullable AccountStatus status) {
    if (status == null) {
      return Specification.unrestricted();
    }

    return (root, query, cb) -> {
      var profileMatches = query.subquery(java.util.UUID.class);
      var profileRoot = profileMatches.from(Profile.class);
      profileMatches.select(profileRoot.get(Profile_.id))
          .where(cb.equal(profileRoot.get(Profile_.user).get(User_.status), status));
      return root.get(Customer_.id).in(profileMatches);
    };
  }

  private static Specification<Customer> profilePointsBetween(@Nullable Long minPoints, @Nullable Long maxPoints) {
    if (minPoints == null && maxPoints == null) {
      return Specification.unrestricted();
    }

    return (root, query, cb) -> {
      var profileMatches = query.subquery(java.util.UUID.class);
      var profileRoot = profileMatches.from(Profile.class);
      var points = profileRoot.get(Profile_.points);
      if (minPoints != null && maxPoints != null) {
        profileMatches.select(profileRoot.get(Profile_.id))
            .where(cb.between(points, minPoints, maxPoints));
        return root.get(Customer_.id).in(profileMatches);
      }
      if (minPoints != null) {
        profileMatches.select(profileRoot.get(Profile_.id))
            .where(cb.greaterThanOrEqualTo(points, minPoints));
        return root.get(Customer_.id).in(profileMatches);
      }
      profileMatches.select(profileRoot.get(Profile_.id))
          .where(cb.lessThanOrEqualTo(points, maxPoints));
      return root.get(Customer_.id).in(profileMatches);
    };
  }
}
