package com.noserbulgaria.micromarket.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuestRepository extends JpaRepository<Guest, UUID> {

  Optional<Guest> findByEmailIgnoreCase(String email);
}
