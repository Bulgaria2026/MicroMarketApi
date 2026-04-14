package com.noserbulgaria.micromarket.domain.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  Page<Order> findAll(Pageable pageable, OrderFilter filter);

  Optional<Order> findById(UUID id);
}
