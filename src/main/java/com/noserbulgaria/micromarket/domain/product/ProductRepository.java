package com.noserbulgaria.micromarket.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends
    JpaRepository<Product, UUID>,
    JpaSpecificationExecutor<Product>,
    RevisionRepository<Product, UUID, Integer> {
  @Modifying
  @Query(value = "DELETE FROM product_aud WHERE id = :productId", nativeQuery = true)
  void deleteAuditHistoryByProductId(@Param("productId") UUID productId);
}
