package com.noserbulgaria.micromarket.generic;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public abstract class ExtendedSpecification<T extends ExtendedEntity> {

  public abstract Specification<T> withFilter();

  protected Specification<T> fromDate(@Nullable Instant from) {
    return (root, _, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  protected Specification<T> toDate(@Nullable Instant to) {
    return (root, _, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

  protected Specification<T> equalTo(@Nullable Object value, String field) {
    return (root, _, cb) -> value == null ? null : cb.equal(root.get(field), value);
  }
}