package com.noserbulgaria.micromarket.domain.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;

  @Override
  public Page<Order> findAll(Pageable pageable, OrderFilterRequest filter) {
    Specification<Order> spec = new OrderSpecification(filter).withFilter();
    return orderRepository.findAll(spec, pageable);
  }

  @Override
  public Optional<Order> findById(UUID id) {
    return orderRepository.findById(id);
  }
}
