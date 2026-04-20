package com.noserbulgaria.micromarket.domain.guest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {

  Optional<Guest> findByEmail(String email);

  boolean existsByEmail(String email);
}
