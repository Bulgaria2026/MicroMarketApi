package com.noserbulgaria.micromarket.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends
    JpaRepository<Product, UUID>,
    JpaSpecificationExecutor<Product>,
    RevisionRepository<Product, UUID, Integer> {

  Page<Product> findAllByEnabledTrue(Pageable pageable);

  Optional<Product> findByNameIgnoreCase(String name);

  List<Product> findAllByEnabledFalse();
}
