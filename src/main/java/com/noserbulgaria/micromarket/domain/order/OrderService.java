package com.noserbulgaria.micromarket.domain.order;

import com.noserbulgaria.micromarket.domain.order.dto.OrderMapper;
import com.noserbulgaria.micromarket.domain.order.dto.OrderResponseDto;
import com.noserbulgaria.micromarket.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;

  public OrderResponseDto findByIdOrThrow(UUID id) {
    return orderRepository.findById(id)
        .map(orderMapper::toDto)
        .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
  }

  public Page<OrderResponseDto> findAll(Specification<Order> spec, Pageable pageable) {
    return orderRepository.findAll(spec, pageable)
        .map(orderMapper::toDto);
  }
}
