package com.noserbulgaria.micromarket.customer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  @EntityGraph(attributePaths = {"customer", "user"})
  Optional<Profile> findByUserId(UUID userId);

  @EntityGraph(attributePaths = {"customer", "user"})
  Optional<Profile> findByCustomer_Email(String email);
}
