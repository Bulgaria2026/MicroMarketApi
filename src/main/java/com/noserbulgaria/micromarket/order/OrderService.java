package com.noserbulgaria.micromarket.order;

import com.noserbulgaria.micromarket.exception.NotFoundApiException;
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

  public OrderResponse findByIdOrThrow(UUID id) {
    return orderRepository.findById(id)
        .map(orderMapper::toDto)
        .orElseThrow(() -> new NotFoundApiException("Order with id '%s' not found".formatted(id)));
  }

  public Page<OrderResponse> findAll(Specification<Order> spec, Pageable pageable) {
    return orderRepository.findAll(spec, pageable)
        .map(orderMapper::toDto);
  }

  public Order findByStripeCheckoutSessionIdOrThrow(String stripeCheckoutSessionId) {
    return orderRepository.findByStripeCheckoutSessionId(stripeCheckoutSessionId)
        .orElseThrow(() -> new NotFoundApiException(
            "Order with Stripe Checkout session id '%s' not found".formatted(stripeCheckoutSessionId)));
  }
}
