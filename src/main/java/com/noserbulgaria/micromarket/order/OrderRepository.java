package com.noserbulgaria.micromarket.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.history.RevisionRepository;

public interface OrderRepository extends
    JpaRepository<Order, UUID>,
    JpaSpecificationExecutor<Order>,
    RevisionRepository<Order, UUID, Integer> {

  Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);
}
