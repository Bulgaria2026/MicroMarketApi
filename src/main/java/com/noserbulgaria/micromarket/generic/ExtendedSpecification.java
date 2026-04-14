package com.noserbulgaria.micromarket.generic;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

public abstract class ExtendedSpecification<T extends ExtendedEntity> {

  public abstract Specification<T> withFilter();

  protected Specification<T> fromDate(Instant from) {
    return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  protected Specification<T> toDate(Instant to) {
    return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

  protected Specification<T> updatedAfter(Instant from) {
    return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("updatedAt"), from);
  }

  protected Specification<T> equalTo(Object value, String field) {
    return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
  }

  protected Specification<T> like(String value, String field) {
    return (root, query, cb) -> value == null ? null : cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
  }

}