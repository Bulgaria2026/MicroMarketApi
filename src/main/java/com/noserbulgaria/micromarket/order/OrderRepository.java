package com.noserbulgaria.micromarket.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends
    JpaRepository<Order, UUID>,
    JpaSpecificationExecutor<Order>,
    RevisionRepository<Order, UUID, Integer> {

  Optional<Order> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

  Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);

}
