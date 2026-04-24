package com.noserbulgaria.micromarket.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends
    JpaRepository<Product, UUID>,
    JpaSpecificationExecutor<Product>,
    RevisionRepository<Product, UUID, Integer> {

  /** Loads enabled products by id. Disabled products are invisible to checkout — same as non-existent. */
  List<Product> findByIdInAndEnabledTrue(Collection<UUID> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id = :id")
  Optional<Product> findByIdForUpdate(@Param("id") UUID id);
}
