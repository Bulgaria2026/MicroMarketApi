package com.noserbulgaria.micromarket.domain.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

/**
 * Implementation of {@link OrderService}.
 */
@Service
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;

  public OrderServiceImpl(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public Page<Order> findAll(int page, int size, Instant fromDate, Instant toDate, UUID customerId, OrderStatusType status) {
    PageRequest pageRequest = PageRequest.of(page, size);
    
    Specification<Order> spec = (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      
      if (fromDate != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
      }
      if (toDate != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
      }
      if (customerId != null) {
        predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      
      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };

    return orderRepository.findAll(spec, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<Order> findById(UUID id) {
    return orderRepository.findById(id);
  }
}
