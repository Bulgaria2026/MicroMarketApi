package com.noserbulgaria.micromarket.customer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

  @EntityGraph(attributePaths = {"profile", "profile.user"})
  Optional<Customer> findByEmail(String email);
}
