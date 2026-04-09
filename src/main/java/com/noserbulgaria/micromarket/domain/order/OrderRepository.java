package com.noserbulgaria.micromarket.domain.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository interface for managing {@link Order} entities.
 *
 * <p>By extending {@link org.springframework.data.jpa.repository.JpaRepository},
 * this repository gets standard CRUD operations, pagination, and sorting support.
 *
 * <p>By extending {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor},
 * it can execute dynamic, type-safe queries built with JPA {@code Specification}s
 * (for example: filtering by optional criteria, combining predicates, and composing
 * complex WHERE clauses at runtime).
 */
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
}
