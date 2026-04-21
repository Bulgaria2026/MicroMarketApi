package com.noserbulgaria.micromarket.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends
    JpaRepository<Product, UUID>,
    JpaSpecificationExecutor<Product>,
    RevisionRepository<Product, UUID, Integer> {

  /** Loads enabled products by id. Disabled products are invisible to checkout — same as non-existent. */
  List<Product> findByIdInAndEnabledTrue(Collection<UUID> ids);

  /** Atomic conditional decrement; returns rows affected ({@code != 1} means insufficient stock or missing product). */
  @Modifying
  @Query("UPDATE Product p SET p.amount = p.amount - :quantity WHERE p.id = :id AND p.amount >= :quantity")
  int tryDecrementStock(@Param("id") UUID id, @Param("quantity") long quantity);
}
