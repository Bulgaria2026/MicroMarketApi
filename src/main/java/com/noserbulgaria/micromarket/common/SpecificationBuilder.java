package com.noserbulgaria.micromarket.common;

import jakarta.persistence.metamodel.SingularAttribute;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Type-safe {@link Specification} factory methods using JPA metamodel attributes.
 */
@UtilityClass
public class SpecificationBuilder {

  public static <T, V extends Comparable<V>> Specification<T> greaterThanOrEqualTo(
      SingularAttribute<? super T, V> attribute, @Nullable V value) {
    return value == null ? Specification.unrestricted()
        : (root, _, cb) -> cb.greaterThanOrEqualTo(root.get(attribute), value);
  }

  public static <T, V extends Comparable<V>> Specification<T> lessThanOrEqualTo(
      SingularAttribute<? super T, V> attribute, @Nullable V value) {
    return value == null ? Specification.unrestricted()
        : (root, _, cb) -> cb.lessThanOrEqualTo(root.get(attribute), value);
  }

  public static <T, V> Specification<T> equalTo(
      SingularAttribute<? super T, V> attribute, @Nullable V value) {
    return value == null ? Specification.unrestricted()
        : (root, _, cb) -> cb.equal(root.get(attribute), value);
  }
}
